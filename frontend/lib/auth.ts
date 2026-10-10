import { EncryptJWT, jwtDecrypt } from "jose"
import { cookies } from "next/headers"
import * as oidc from "openid-client"

// Zitadel is the only place users sign in, the frontend keeps the tokens of the signed in user
// in an encrypted cookie and sends the access token to the backend, it has no user table of its own

export const SESSION_COOKIE = "labs101_session"
export const LOGIN_COOKIE = "labs101_login"

export interface User {
    id: string
    name: string
    email: string
    image: string | null
}

export interface Session {
    user: User
    accessToken: string
    refreshToken: string | null
    idToken: string | null
    // seconds since epoch
    expiresAt: number
}

/** What the login has to remember between the redirect to Zitadel and the callback. */
interface LoginState {
    codeVerifier: string
    state: string
    returnTo: string
}

const ISSUER = process.env.AUTH_ISSUER ?? "http://localhost:8081"
const CLIENT_ID = process.env.AUTH_CLIENT_ID ?? ""
const CLIENT_SECRET = process.env.AUTH_CLIENT_SECRET ?? ""
// public URL of the frontend, behind the reverse proxy request.url only knows the container
const ORG_ID = process.env.ZITADEL_ORG_ID ?? ""

/**
 * Zitadel serves the profile picture of a user at a fixed URL (like GitHub avatars), a new picture shows up there
 * without any sync. Users without one get a 404, the avatar shows the initial then.
 */
export const avatarUrl = (userId: string) =>
    ORG_ID ? `${ISSUER.replace(/\/$/, "")}/assets/v1/${ORG_ID}/users/${userId}/avatar` : null

// own profile in the zitadel console, name and picture are changed there
export const PROFILE_URL = `${ISSUER.replace(/\/$/, "")}/ui/console/users/me`
export const APP_URL = (process.env.APP_URL ?? "http://localhost:3000").replace(/\/$/, "")
export const CALLBACK_URL = `${APP_URL}/auth/callback`

// offline_access for the refresh token
const SCOPE = "openid profile email offline_access"

// refresh a minute early, so the token doesn't run out between the check and the backend request
const REFRESH_MARGIN_SECONDS = 60
const SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 30

let configuration: Promise<oidc.Configuration> | null = null

export function oidcConfiguration(): Promise<oidc.Configuration> {
    configuration ??= oidc.discovery(new URL(ISSUER), CLIENT_ID, CLIENT_SECRET, undefined, {
        // only local development runs zitadel without TLS
        execute: ISSUER.startsWith("http://") ? [oidc.allowInsecureRequests] : [],
    }).catch((e) => {
        // try again on the next request instead of caching the failure
        configuration = null
        throw e
    })
    return configuration
}

async function key(): Promise<Uint8Array> {
    const secret = process.env.AUTH_SECRET
    if (!secret) throw new Error("AUTH_SECRET is not set")
    return new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret)))
}

async function encrypt(payload: object, maxAgeSeconds: number): Promise<string> {
    return new EncryptJWT({ ...payload })
        .setProtectedHeader({ alg: "dir", enc: "A256GCM" })
        .setIssuedAt()
        .setExpirationTime(`${maxAgeSeconds}s`)
        .encrypt(await key())
}

async function decrypt<T>(value: string | undefined): Promise<T | null> {
    if (!value) return null
    try {
        const { payload } = await jwtDecrypt(value, await key())
        return payload as T
    } catch {
        // tampered, expired or encrypted with an old secret
        return null
    }
}

export const cookieOptions = (maxAge: number) => ({
    httpOnly: true,
    secure: APP_URL.startsWith("https://"),
    sameSite: "lax" as const,
    path: "/",
    maxAge,
})

export const encryptSession = (session: Session) => encrypt(session, SESSION_MAX_AGE_SECONDS)
export const decryptSession = (value: string | undefined) => decrypt<Session>(value)
export const sessionCookieOptions = () => cookieOptions(SESSION_MAX_AGE_SECONDS)

export const encryptLoginState = (state: LoginState) => encrypt(state, 600)
export const decryptLoginState = (value: string | undefined) => decrypt<LoginState>(value)

export const needsRefresh = (session: Session) => session.expiresAt - REFRESH_MARGIN_SECONDS <= Date.now() / 1000

/**
 * Session of the tokens Zitadel just issued. Name and email come from the userinfo endpoint, the id token only
 * carries them when "User Info inside ID Token" is enabled in Zitadel. The picture isn't stored, see getUser.
 */
export async function sessionFromTokens(tokens: oidc.TokenEndpointResponse & oidc.TokenEndpointResponseHelpers, previous?: Session): Promise<Session> {
    const claims = tokens.claims()
    const subject = claims?.sub ?? previous?.user.id
    const info = subject ? await userInfo(tokens.access_token, subject) : null
    const merged = { ...claims, ...info }
    const user: User = merged.sub
        ? {
            id: merged.sub,
            name: String(merged.name ?? merged.preferred_username ?? merged.email ?? ""),
            email: String(merged.email ?? ""),
            image: null,
        }
        : previous!.user
    return {
        user,
        accessToken: tokens.access_token,
        // zitadel rotates refresh tokens, but keep the old one if none came back
        refreshToken: tokens.refresh_token ?? previous?.refreshToken ?? null,
        idToken: tokens.id_token ?? previous?.idToken ?? null,
        expiresAt: Math.floor(Date.now() / 1000) + (tokens.expiresIn() ?? 300),
    }
}

// a failing userinfo request must not break the login, the id token is used then
async function userInfo(accessToken: string, subject: string): Promise<oidc.UserInfoResponse | null> {
    try {
        return await oidc.fetchUserInfo(await oidcConfiguration(), accessToken, subject)
    } catch (e) {
        console.error("Loading the userinfo failed", e)
        return null
    }
}

/** New tokens for an expiring session, null if the refresh token isn't valid anymore. */
export async function refreshSession(session: Session): Promise<Session | null> {
    if (!session.refreshToken) return null
    try {
        const tokens = await oidc.refreshTokenGrant(await oidcConfiguration(), session.refreshToken)
        return await sessionFromTokens(tokens, session)
    } catch (e) {
        console.error("Refreshing the session failed", e)
        return null
    }
}

export async function loginUrl(returnTo: string): Promise<{ url: URL, state: LoginState }> {
    const config = await oidcConfiguration()
    const state: LoginState = {
        codeVerifier: oidc.randomPKCECodeVerifier(),
        state: oidc.randomState(),
        returnTo: returnTo.startsWith("/") && !returnTo.startsWith("//") ? returnTo : "/",
    }
    const url = oidc.buildAuthorizationUrl(config, {
        redirect_uri: CALLBACK_URL,
        scope: SCOPE,
        code_challenge: await oidc.calculatePKCECodeChallenge(state.codeVerifier),
        code_challenge_method: "S256",
        state: state.state,
    })
    return { url, state }
}

/**
 * The signed in user, null without a session. The proxy already refreshed the tokens,
 * server components can't write cookies themselves.
 */
export async function getSession(): Promise<Session | null> {
    return decryptSession((await cookies()).get(SESSION_COOKIE)?.value)
}

/** The picture is added here, so sessions from before a config change get the right URL too. */
export async function getUser(): Promise<User | null> {
    const user = (await getSession())?.user
    return user ? { ...user, image: avatarUrl(user.id) } : null
}

/** The access token for the backend, throws without a session. */
export async function getAccessToken(): Promise<string> {
    const session = await getSession()
    if (!session) throw new Error("User is currently not in a session")
    return session.accessToken
}

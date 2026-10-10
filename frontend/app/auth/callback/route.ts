import {
    APP_URL, CALLBACK_URL, decryptLoginState, encryptSession, LOGIN_COOKIE, oidcConfiguration,
    SESSION_COOKIE, sessionCookieOptions, sessionFromTokens
} from "@/lib/auth"
import { NextRequest, NextResponse } from "next/server"
import * as oidc from "openid-client"

export async function GET(request: NextRequest) {
    const login = await decryptLoginState(request.cookies.get(LOGIN_COOKIE)?.value)
    if (!login) {
        // login took too long or was started in another browser
        return NextResponse.redirect(`${APP_URL}/sign-in?error=expired`)
    }

    try {
        // the url zitadel redirected to, request.url is the internal one behind the reverse proxy
        const currentUrl = new URL(CALLBACK_URL)
        currentUrl.search = request.nextUrl.search

        const tokens = await oidc.authorizationCodeGrant(await oidcConfiguration(), currentUrl, {
            pkceCodeVerifier: login.codeVerifier,
            expectedState: login.state,
        })

        const response = NextResponse.redirect(`${APP_URL}${login.returnTo}`)
        response.cookies.set(SESSION_COOKIE, await encryptSession(sessionFromTokens(tokens)), sessionCookieOptions())
        response.cookies.delete(LOGIN_COOKIE)
        return response
    } catch (e) {
        console.error("Sign in failed", e)
        return NextResponse.redirect(`${APP_URL}/sign-in?error=failed`)
    }
}

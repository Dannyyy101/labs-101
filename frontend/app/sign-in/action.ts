'use server'

import { APP_URL, getSession, oidcConfiguration, SESSION_COOKIE } from "@/lib/auth"
import { cookies } from "next/headers"
import { redirect } from "next/navigation"
import * as oidc from "openid-client"

// ends the session here and at zitadel, otherwise the next sign in would happen without asking
export async function signOut() {
    const session = await getSession()
    ;(await cookies()).delete(SESSION_COOKIE)

    let url = `${APP_URL}/sign-in`
    try {
        url = oidc.buildEndSessionUrl(await oidcConfiguration(), {
            post_logout_redirect_uri: url,
            ...(session?.idToken ? { id_token_hint: session.idToken } : {}),
        }).href
    } catch (e) {
        // zitadel not reachable, at least the local session is gone
        console.error("Signing out at zitadel failed", e)
    }
    redirect(url)
}

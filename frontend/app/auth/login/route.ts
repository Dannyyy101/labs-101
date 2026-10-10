import { cookieOptions, encryptLoginState, LOGIN_COOKIE, loginUrl } from "@/lib/auth"
import { NextRequest, NextResponse } from "next/server"

// redirects to the login of zitadel, it comes back to /auth/callback
export async function GET(request: NextRequest) {
    const { url, state } = await loginUrl(request.nextUrl.searchParams.get("returnTo") ?? "/")

    const response = NextResponse.redirect(url)
    response.cookies.set(LOGIN_COOKIE, await encryptLoginState(state), cookieOptions(600))
    return response
}

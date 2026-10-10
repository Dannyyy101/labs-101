import { NextRequest, NextResponse } from "next/server";
import {
    decryptSession, encryptSession, needsRefresh, refreshSession, SESSION_COOKIE, sessionCookieOptions
} from "@/lib/auth";

// every page and server action passes here, so this is the one place that refreshes the tokens:
// server components and the backend requests only ever read the session cookie
export async function proxy(request: NextRequest) {
    let session = await decryptSession(request.cookies.get(SESSION_COOKIE)?.value)

    if (session && needsRefresh(session)) {
        session = await refreshSession(session)
        if (session) {
            const value = await encryptSession(session)
            // the new cookie for this request, the page renders after the proxy
            request.cookies.set(SESSION_COOKIE, value)
            const response = NextResponse.next({ request: { headers: request.headers } })
            // and for the browser
            response.cookies.set(SESSION_COOKIE, value, sessionCookieOptions())
            return response
        }
    }

    if (!session) {
        const signIn = new URL("/sign-in", request.url)
        signIn.searchParams.set("returnTo", request.nextUrl.pathname + request.nextUrl.search)
        const response = NextResponse.redirect(signIn);
        response.cookies.delete(SESSION_COOKIE)
        return response
    }

    return NextResponse.next();
}

export const config = {
    matcher: ['/((?!api|auth|sign-in|_next/static|_next/image|favicon.ico).*)',],
};

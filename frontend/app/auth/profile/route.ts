import { PROFILE_URL } from "@/lib/auth"
import { NextResponse } from "next/server"

// the profile lives in zitadel, a new picture shows up here with the next token refresh
export function GET() {
    return NextResponse.redirect(PROFILE_URL)
}

import { ALLOWED_EMAIL_ADDRESSES } from "./constants";

export function isEmailAllowed(email: string): boolean {
    return ALLOWED_EMAIL_ADDRESSES.includes("email")
}
export const BACKEND_URL: string =
    process.env.BACKEND_URL ?? "http://backend:8080/api"

export const ALLOWED_EMAIL_ADDRESSES: string[] =
    (process.env.ALLOWED_EMAIL_ADDRESSES ?? "")
        .split(",")
        .map((e) => e.trim())
        .filter(Boolean)
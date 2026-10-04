export const BACKEND_URL: string =
    process.env.BACKEND_URL ?? "http://backend:8080/api"

export const ALLOWED_EMAIL_ADDRESSES: string[] =
    (process.env.ALLOWED_EMAIL_ADDRESSES ?? "")
        .split(",")
        .map((e) => e.trim())
        .filter(Boolean)

// shared key the backend expects in the X-API-Key header, only ever read on the server
export const API_KEY: string = process.env.API_KEY ?? ""

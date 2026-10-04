import { API_KEY } from "./constants"

// fetch against the backend, every request has to carry the API key
export function backendFetch(input: string | URL, init?: RequestInit): Promise<Response> {
    const headers = new Headers(init?.headers)
    headers.set("X-API-Key", API_KEY)
    return fetch(input, { ...init, headers })
}

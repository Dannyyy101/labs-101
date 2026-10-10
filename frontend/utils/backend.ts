import { getAccessToken } from "@/lib/auth"

// fetch against the backend, every request carries the access token of the signed in user
export async function backendFetch(input: string | URL, init?: RequestInit): Promise<Response> {
    const headers = new Headers(init?.headers)
    headers.set("Authorization", `Bearer ${await getAccessToken()}`)
    return fetch(input, { ...init, headers })
}

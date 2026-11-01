import createClient, { type Middleware } from "openapi-fetch"
import { getAccessToken } from "@/lib/auth"
import { BACKEND_URL } from "@/utils/constants"
import { ApiError } from "@/utils/types/api"
import { Result } from "@/utils/types/result"
import type { paths } from "./schema"

// every request carries the access token of the signed in user
const auth: Middleware = {
    async onRequest({ request }) {
        request.headers.set("Authorization", `Bearer ${await getAccessToken()}`)
        return request
    },
}

/**
 * Typed client of the backend, paths, parameters and bodies come from api/openapi.json
 * (`npm run generate:api`), so a changed DTO breaks the build instead of failing at runtime.
 */
// the paths of the spec start with /api, BACKEND_URL ends with it
export const api = createClient<paths>({ baseUrl: BACKEND_URL.replace(/\/api\/?$/, ""), cache: "no-store" })
api.use(auth)

/** The localized message of the backend if there is one ("Du hast dieses Training schon angefeuert"). */
export function errorMessage(error: unknown, fallback: string): string {
    return (error as ApiError | undefined)?.errorMessage || fallback
}

type Answer<T> = { data?: T, error?: unknown, response: Response }

/**
 * For actions that must not throw, e.g. the ones of the navbar: the data of the answer or the
 * localized message of the backend, `failure` when it sent none.
 */
export async function toResult<T>(call: () => Promise<Answer<T>>, failure: string): Promise<Result<T>> {
    try {
        const { data, error, response } = await call()
        if (!response.ok) {
            console.error(`${failure}: ${response.status}`)
            return { ok: false, error: errorMessage(error, failure) }
        }
        return { ok: true, data: data as T }
    } catch (e) {
        // backend not reachable or no session
        console.error(failure, e)
        return { ok: false, error: `${failure}, der Server ist nicht erreichbar` }
    }
}

'use server'

import { auth } from "@/lib/auth"
import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { ApiError } from "@/utils/types/api"
import { AppNotification, CreateNotification } from "@/utils/types/notification"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"
import { headers } from "next/headers"

// the bell is part of every page, so none of these actions throw: a failing backend
// must not take down the navbar, they get a Result and decide themselves what to show

async function notificationsUrl(path = ""): Promise<string> {
    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")

    return `${BACKEND_URL}/users/${session.user.id}/notifications${path}`
}

// the localized message of the backend if there is one
async function errorMessage(response: Response, fallback: string): Promise<string> {
    const apiError = await response.json().catch(() => null) as ApiError | null
    return apiError?.errorMessage || fallback
}

async function request<T>(path: string, init: RequestInit, failure: string, parse: (response: Response) => Promise<T>): Promise<Result<T>> {
    try {
        const response = await backendFetch(await notificationsUrl(path), { ...init, cache: 'no-store' })
        if (response.ok) {
            return { ok: true, data: await parse(response) }
        }
        console.error(`${failure}: ${response.status}`)
        return { ok: false, error: await errorMessage(response, failure) }
    } catch (e) {
        // backend not reachable or no session
        console.error(failure, e)
        return { ok: false, error: `${failure}, der Server ist nicht erreichbar` }
    }
}

const json = <T,>(response: Response) => response.json() as Promise<T>
const none = async () => undefined

export async function getNotifications(options: { unreadOnly?: boolean, limit?: number } = {}): Promise<Result<AppNotification[]>> {
    const params = new URLSearchParams()
    if (options.unreadOnly) params.set("unreadOnly", "true")
    if (options.limit) params.set("limit", String(options.limit))
    const query = params.size ? `?${params}` : ""

    return request(query, {}, "Benachrichtigungen konnten nicht geladen werden", json<AppNotification[]>)
}

export async function getUnreadCount(): Promise<Result<number>> {
    return request("/unread-count", {}, "Ungelesene Benachrichtigungen konnten nicht geladen werden",
        async (response) => (await json<{ count: number }>(response)).count)
}

/**
 * Creates a notification for the current user, e.g. from another server action:
 * `createNotification({ title: "Training gespeichert", type: "SUCCESS", link: "/workouts/3", linkLabel: "Ansehen" })`
 */
export async function createNotification(notification: CreateNotification): Promise<Result<AppNotification>> {
    return request("", {
        method: "POST",
        body: JSON.stringify(notification),
        headers: { 'Content-Type': 'application/json' },
    }, "Benachrichtigung konnte nicht erstellt werden", json<AppNotification>)
}

export async function markRead(id: number, read = true): Promise<Result<AppNotification>> {
    const result = await request(`/${id}/read`, { method: read ? "PUT" : "DELETE" },
        "Benachrichtigung konnte nicht aktualisiert werden", json<AppNotification>)
    if (result.ok) revalidatePath("/notifications")
    return result
}

export async function markAllRead(): Promise<Result<void>> {
    const result = await request("/read", { method: "PUT" }, "Benachrichtigungen konnten nicht aktualisiert werden", none)
    if (result.ok) revalidatePath("/notifications")
    return result
}

export async function deleteNotification(id: number): Promise<Result<void>> {
    const result = await request(`/${id}`, { method: "DELETE" }, "Benachrichtigung konnte nicht gelöscht werden", none)
    if (result.ok) revalidatePath("/notifications")
    return result
}

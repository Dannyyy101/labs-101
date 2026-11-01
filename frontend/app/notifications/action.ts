'use server'

import { api, toResult } from "@/lib/api/client"
import { AppNotification, CreateNotification } from "@/utils/types/notification"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

// the bell is part of every page, so none of these actions throw: a failing backend
// must not take down the navbar, they get a Result and decide themselves what to show

export async function getNotifications(options: { unreadOnly?: boolean, limit?: number } = {}): Promise<Result<AppNotification[]>> {
    return toResult(() => api.GET("/api/users/me/notifications", {
        params: { query: { unreadOnly: options.unreadOnly || undefined, limit: options.limit || undefined } },
    }), "Benachrichtigungen konnten nicht geladen werden")
}

export async function getUnreadCount(): Promise<Result<number>> {
    const result = await toResult(() => api.GET("/api/users/me/notifications/unread-count"),
        "Ungelesene Benachrichtigungen konnten nicht geladen werden")
    return result.ok ? { ok: true, data: result.data.count } : result
}

/**
 * Creates a notification for the current user, e.g. from another server action:
 * `createNotification({ title: "Training gespeichert", type: "SUCCESS", link: "/workouts/3", linkLabel: "Ansehen" })`
 */
export async function createNotification(notification: CreateNotification): Promise<Result<AppNotification>> {
    return toResult(() => api.POST("/api/users/me/notifications", { body: notification }),
        "Benachrichtigung konnte nicht erstellt werden")
}

export async function markRead(id: number, read = true): Promise<Result<AppNotification>> {
    const options = { params: { path: { id } } }
    const result = await toResult(() => read
        ? api.PUT("/api/users/me/notifications/{id}/read", options)
        : api.DELETE("/api/users/me/notifications/{id}/read", options),
        "Benachrichtigung konnte nicht aktualisiert werden")
    if (result.ok) revalidatePath("/notifications")
    return result
}

export async function markAllRead(): Promise<Result<void>> {
    const result = await toResult(() => api.PUT("/api/users/me/notifications/read"),
        "Benachrichtigungen konnten nicht aktualisiert werden")
    if (result.ok) revalidatePath("/notifications")
    return result
}

export async function deleteNotification(id: number): Promise<Result<void>> {
    const result = await toResult(() => api.DELETE("/api/users/me/notifications/{id}", { params: { path: { id } } }),
        "Benachrichtigung konnte nicht gelöscht werden")
    if (result.ok) revalidatePath("/notifications")
    return result
}

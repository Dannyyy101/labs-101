'use client'

import { deleteNotification, getNotifications, markAllRead, markRead } from "@/app/notifications/action"
import { AppNotification } from "@/utils/types/notification"
import { useCallback, useState } from "react"

// list state with optimistic updates, a failed request loads the list again
export function useNotifications(initial: AppNotification[] = []) {
    const [notifications, setNotifications] = useState(initial)
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    const load = useCallback(async (limit?: number) => {
        setLoading(true)
        const result = await getNotifications({ limit })
        setLoading(false)
        if (result.ok) {
            setNotifications(result.data)
            setError(null)
        } else {
            setError(result.error)
        }
    }, [])

    const update = useCallback(async (change: (list: AppNotification[]) => AppNotification[], send: () => Promise<{ ok: boolean }>) => {
        setNotifications(change)
        if (!(await send()).ok) await load()
    }, [load])

    const now = () => new Date().toISOString()

    return {
        notifications,
        error,
        loading,
        load,
        unreadCount: notifications.filter((n) => n.readAt === null).length,
        setRead: (id: number, read: boolean) => update(
            (list) => list.map((n) => n.id === id ? { ...n, readAt: read ? (n.readAt ?? now()) : null } : n),
            () => markRead(id, read)),
        readAll: () => update(
            (list) => list.map((n) => ({ ...n, readAt: n.readAt ?? now() })),
            markAllRead),
        remove: (id: number) => update(
            (list) => list.filter((n) => n.id !== id),
            () => deleteNotification(id)),
    }
}

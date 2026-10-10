'use client'

import { getUnreadCount } from "@/app/notifications/action"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Spinner } from "@/components/ui/spinner"
import { Bell } from "lucide-react"
import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import NotificationItem from "./NotificationItem"
import { useNotifications } from "./useNotifications"

// how often the badge asks the backend for new notifications
const POLL_INTERVAL_MS = 60_000
const POPOVER_LIMIT = 20

export function NotificationBell() {
    const [open, setOpen] = useState(false)
    const [unread, setUnread] = useState(0)
    const { notifications, error, loading, load, unreadCount, setRead, readAll, remove } = useNotifications()

    const refreshCount = useCallback(async () => {
        const result = await getUnreadCount()
        // a failing backend just keeps the last count, the navbar must keep working
        if (result.ok) setUnread(result.data)
    }, [])

    useEffect(() => {
        // the state is only set after the request, not synchronously
        // eslint-disable-next-line react-hooks/set-state-in-effect
        refreshCount()
        const interval = setInterval(() => {
            if (document.visibilityState === "visible") refreshCount()
        }, POLL_INTERVAL_MS)
        const onFocus = () => refreshCount()
        window.addEventListener("focus", onFocus)
        return () => {
            clearInterval(interval)
            window.removeEventListener("focus", onFocus)
        }
    }, [refreshCount])

    // while open the loaded list is the source of the badge
    const badge = open && !loading && !error ? unreadCount : unread

    const onOpenChange = (next: boolean) => {
        setOpen(next)
        if (next) load(POPOVER_LIMIT)
        else refreshCount()
    }

    return <Popover open={open} onOpenChange={onOpenChange}>
        <PopoverTrigger aria-label={badge ? `Benachrichtigungen, ${badge} ungelesen` : "Benachrichtigungen"}
            className="relative flex size-10 items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground">
            <Bell className="size-5" />
            {badge > 0 && <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold leading-none text-white tabular-nums">
                {badge > 99 ? "99+" : badge}
            </span>}
        </PopoverTrigger>
        <PopoverContent align="end" className="w-[min(24rem,calc(100vw-2rem))] gap-2 p-2">
            <div className="flex items-center justify-between px-2 pt-1">
                <h2 className="text-base font-semibold">Benachrichtigungen</h2>
                {unreadCount > 0 && <button type="button" onClick={readAll} className="text-sm text-blue-500 hover:underline">Alle gelesen</button>}
            </div>

            <div className="max-h-[min(28rem,70vh)] overflow-y-auto">
                {loading && notifications.length === 0
                    ? <div className="flex justify-center py-8"><Spinner /></div>
                    : error
                        ? <p role="alert" className="px-2 py-6 text-center text-sm text-muted-foreground">{error}</p>
                        : notifications.length === 0
                            ? <p className="px-2 py-8 text-center text-sm text-muted-foreground">Keine Benachrichtigungen</p>
                            : <ul className="flex flex-col">
                                {notifications.map((notification) => <NotificationItem
                                    key={notification.id}
                                    notification={notification}
                                    onRead={(read) => setRead(notification.id, read)}
                                    onDelete={() => remove(notification.id)}
                                    onNavigate={() => onOpenChange(false)}
                                />)}
                            </ul>}
            </div>

            <Link href="/notifications" onClick={() => onOpenChange(false)}
                className="rounded-xl py-2 text-center text-sm font-medium text-blue-500 hover:bg-muted">
                Alle anzeigen
            </Link>
        </PopoverContent>
    </Popover>
}

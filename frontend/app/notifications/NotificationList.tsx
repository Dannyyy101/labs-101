'use client'

import NotificationItem from "@/components/notifications/NotificationItem"
import { useNotifications } from "@/components/notifications/useNotifications"
import { Button } from "@/components/ui/button"
import { AppNotification } from "@/utils/types/notification"
import { CheckCheck } from "lucide-react"

export default function NotificationList({ initial }: { initial: AppNotification[] }) {
    const { notifications, unreadCount, setRead, readAll, remove } = useNotifications(initial)

    return <div className="rounded-3xl bg-card p-2 shadow-sm">
        <div className="flex items-center justify-between px-3 py-2">
            <p className="text-sm text-muted-foreground">{unreadCount === 0 ? "Alles gelesen" : `${unreadCount} ungelesen`}</p>
            <Button variant="ghost" size="sm" disabled={unreadCount === 0} onClick={readAll}><CheckCheck />Alle gelesen</Button>
        </div>
        {notifications.length === 0
            ? <p className="px-3 py-10 text-center text-muted-foreground">Keine Benachrichtigungen</p>
            : <ul className="flex flex-col">
                {notifications.map((notification) => <NotificationItem
                    key={notification.id}
                    notification={notification}
                    onRead={(read) => setRead(notification.id, read)}
                    onDelete={() => remove(notification.id)}
                />)}
            </ul>}
    </div>
}

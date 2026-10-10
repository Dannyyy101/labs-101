'use client'

import { AppNotification, NotificationType } from "@/utils/types/notification"
import { formatDistanceToNow } from "date-fns"
import { de } from "date-fns/locale"
import { AlertTriangle, ArrowUpRight, Check, CheckCircle2, Info, Trash2, XCircle } from "lucide-react"
import Link from "next/link"

const TYPE_STYLE: Record<NotificationType, { icon: typeof Info, className: string }> = {
    INFO: { icon: Info, className: "text-blue-500 bg-blue-500/10" },
    SUCCESS: { icon: CheckCircle2, className: "text-green-600 bg-green-500/10" },
    WARNING: { icon: AlertTriangle, className: "text-amber-500 bg-amber-500/10" },
    ERROR: { icon: XCircle, className: "text-destructive bg-destructive/10" },
}

// paths of the app stay in the tab, urls to other sites open in a new one
function NotificationLink({ notification, onOpen }: { notification: AppNotification, onOpen: () => void }) {
    const label = notification.linkLabel ?? "Öffnen"
    const className = "inline-flex items-center gap-0.5 text-sm font-medium text-blue-500 hover:underline"

    if (notification.link!.startsWith("/"))
        return <Link href={notification.link!} onClick={onOpen} className={className}>{label}</Link>

    return <a href={notification.link!} target="_blank" rel="noopener noreferrer" onClick={onOpen} className={className}>
        {label}<ArrowUpRight className="size-3.5" />
    </a>
}

export default function NotificationItem({ notification, onRead, onDelete, onNavigate }: {
    notification: AppNotification
    onRead: (read: boolean) => void
    onDelete: () => void
    // e.g. to close the popover
    onNavigate?: () => void
}) {
    const { icon: Icon, className } = TYPE_STYLE[notification.type] ?? TYPE_STYLE.INFO
    const unread = notification.readAt === null

    const open = () => {
        if (unread) onRead(true)
        onNavigate?.()
    }

    return <li className="group flex gap-3 rounded-2xl p-3 hover:bg-muted/60">
        <span className={`flex size-8 shrink-0 items-center justify-center rounded-full ${className}`}>
            <Icon className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
            <div className="flex items-start gap-2">
                <p className={`flex-1 break-words ${unread ? "font-semibold" : "font-medium text-muted-foreground"}`}>{notification.title}</p>
                {unread && <span className="mt-1.5 size-2 shrink-0 rounded-full bg-blue-500" aria-label="Ungelesen" />}
            </div>
            {notification.message && <p className="mt-0.5 whitespace-pre-line break-words text-sm text-muted-foreground">{notification.message}</p>}
            <div className="mt-1.5 flex items-center gap-3">
                <time dateTime={notification.createdAt} className="text-xs text-muted-foreground">
                    {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true, locale: de })}
                </time>
                {notification.link && <NotificationLink notification={notification} onOpen={open} />}
                {/* visible on touch screens, they have no hover */}
                <span className="ml-auto flex gap-1 md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100">
                    <button type="button" onClick={() => onRead(unread)} aria-label={unread ? "Als gelesen markieren" : "Als ungelesen markieren"}
                        title={unread ? "Als gelesen markieren" : "Als ungelesen markieren"}
                        className="rounded-lg p-1 text-muted-foreground hover:bg-muted hover:text-foreground">
                        <Check className="size-4" />
                    </button>
                    <button type="button" onClick={onDelete} aria-label="Löschen" title="Löschen"
                        className="rounded-lg p-1 text-muted-foreground hover:bg-muted hover:text-destructive">
                        <Trash2 className="size-4" />
                    </button>
                </span>
            </div>
        </div>
    </li>
}

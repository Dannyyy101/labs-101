'use client'

import { AppNotification, NotificationType } from "@/utils/types/notification"
import { formatDistanceToNow } from "date-fns"
import { de } from "date-fns/locale"
import { AlertTriangle, ArrowUpRight, Check, CheckCircle2, Info, Trash2, XCircle } from "lucide-react"
import Link from "next/link"

const TYPE_STYLE: Record<NotificationType, { icon: typeof Info, className: string }> = {
    INFO: { icon: Info, className: "text-brand-strong bg-brand-soft" },
    SUCCESS: { icon: CheckCircle2, className: "text-success bg-success-soft" },
    WARNING: { icon: AlertTriangle, className: "text-warning bg-warning-soft" },
    ERROR: { icon: XCircle, className: "text-destructive bg-destructive/10" },
}

// paths of the app stay in the tab, urls to other sites open in a new one
function NotificationLink({ notification, onOpen }: { notification: AppNotification, onOpen: () => void }) {
    const label = notification.linkLabel ?? "Öffnen"
    const className = "inline-flex items-center gap-0.5 text-body font-medium text-brand-strong hover:underline"

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

    return <li className="group flex gap-3 rounded-[12px] p-3 hover:bg-surface-raised/50 dark:hover:bg-white/5">
        <span className={`flex size-8 shrink-0 items-center justify-center rounded-full ${className}`}>
            <Icon className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
            <div className="flex items-start gap-2">
                <p className={`flex-1 break-words ${unread ? "font-semibold" : "font-medium text-ink-muted"}`}>{notification.title}</p>
                {unread && <span className="mt-1.5 size-2 shrink-0 rounded-full bg-brand" aria-label="Ungelesen" />}
            </div>
            {notification.message && <p className="mt-0.5 whitespace-pre-line break-words text-body text-ink-muted">{notification.message}</p>}
            <div className="mt-1.5 flex items-center gap-3">
                <time dateTime={notification.createdAt} className="text-label text-ink-muted">
                    {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true, locale: de })}
                </time>
                {notification.link && <NotificationLink notification={notification} onOpen={open} />}
                {/* visible on touch screens, they have no hover */}
                <span className="ml-auto flex gap-1 md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100">
                    <button type="button" onClick={() => onRead(unread)} aria-label={unread ? "Als gelesen markieren" : "Als ungelesen markieren"}
                        title={unread ? "Als gelesen markieren" : "Als ungelesen markieren"}
                        className="rounded-lg p-1 text-ink-muted hover:bg-surface-raised/50 dark:hover:bg-white/5 hover:text-foreground">
                        <Check className="size-4" />
                    </button>
                    <button type="button" onClick={onDelete} aria-label="Löschen" title="Löschen"
                        className="rounded-lg p-1 text-ink-muted hover:bg-surface-raised/50 dark:hover:bg-white/5 hover:text-destructive">
                        <Trash2 className="size-4" />
                    </button>
                </span>
            </div>
        </div>
    </li>
}

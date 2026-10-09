// decides icon and color, see NotificationItem
export type NotificationType = "INFO" | "SUCCESS" | "WARNING" | "ERROR"

export interface AppNotification {
    id: number
    type: NotificationType
    title: string
    message: string | null
    // path of the app ("/workouts/3") or an http(s) url, null when there is nothing to open
    link: string | null
    linkLabel: string | null
    createdAt: string
    // null while unread
    readAt: string | null
}

// everything except the title is optional
export interface CreateNotification {
    title: string
    type?: NotificationType
    message?: string
    link?: string
    linkLabel?: string
}

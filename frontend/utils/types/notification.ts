import type { Schemas } from "@/lib/api/types"

export type AppNotification = Schemas["NotificationDto"]
// decides icon and color, see NotificationItem
export type NotificationType = AppNotification["type"]
// everything except the title is optional
export type CreateNotification = Schemas["CreateNotificationDto"]

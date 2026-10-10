import ErrorState from "@/components/ErrorState"
import { getNotifications } from "./action"
import NotificationList from "./NotificationList"

export default async function Notifications() {
    const notifications = await getNotifications({ limit: 200 })

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-2xl flex-col gap-5">
            <h1 className="text-[34px] font-bold leading-tight tracking-tight">Benachrichtigungen</h1>
            {notifications.ok
                ? <NotificationList initial={notifications.data} />
                : <ErrorState title="Benachrichtigungen nicht verfügbar" message={notifications.error} />}
        </div>
    </div>
}

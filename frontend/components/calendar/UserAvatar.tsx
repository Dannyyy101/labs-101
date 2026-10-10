import { AvatarImage } from "@/components/AvatarImage"
import { cn } from "@/lib/utils"
import { CalendarUser } from "@/utils/types/calendarTypes"

/** Profile picture, the first letter of the name without one. */
export function UserAvatar({ user, className }: { user: CalendarUser, className?: string }) {
    const initial = user.name?.trim().charAt(0).toUpperCase() || "?"
    return <span title={user.name ?? undefined} className={cn("flex size-6 shrink-0 items-center justify-center overflow-hidden rounded-full bg-muted-foreground/70 text-[10px] font-semibold text-white ring-2 ring-background", className)}>
        <AvatarImage src={user.image} alt={user.name ?? ""} size={32} className="size-full object-cover" fallback={initial} />
    </span>
}

/** Overlapping pictures, the rest as "+n". */
export function AvatarStack({ users, max = 3, className }: { users: CalendarUser[], max?: number, className?: string }) {
    const shown = users.slice(0, max)
    const rest = users.length - shown.length
    return <div className={cn("flex -space-x-1.5", className)}>
        {shown.map((user) => <UserAvatar key={user.id} user={user} className="size-5" />)}
        {rest > 0 && <span className="flex size-5 items-center justify-center rounded-full bg-muted text-[10px] font-semibold text-foreground ring-2 ring-background">+{rest}</span>}
    </div>
}

'use client'

import { useNow } from "@/app/workouts/useNow"
import { cn } from "@/lib/utils"
import { Training } from "@/utils/types/friend"
import { Check, Flame } from "lucide-react"
import { useState, useTransition } from "react"
import { cheerFriend } from "./action"

/** "Trainiert gerade · Push · 23 Min" with a pulsing dot. */
export function TrainingBadge({ training, className }: { training: Training, className?: string }) {
    const now = useNow(30_000)
    const minutes = Math.max(0, Math.floor((now - Date.parse(training.startedAt)) / 60_000))

    return <span className={cn("flex min-w-0 items-center gap-1.5 text-sm font-medium text-[#30d158]", className)}>
        <span className="relative flex size-2 shrink-0">
            <span className="absolute inline-flex size-full animate-ping rounded-full bg-[#30d158] opacity-75" />
            <span className="relative inline-flex size-2 rounded-full bg-[#30d158]" />
        </span>
        <span className="truncate">
            Trainiert gerade{training.name && ` · ${training.name}`} · {minutes < 60 ? `${minutes} Min` : `${Math.floor(minutes / 60)} Std ${minutes % 60} Min`}
        </span>
    </span>
}

/** Once per workout of the friend, afterwards it stays checked. */
export function CheerButton({ friendId, training, large }: { friendId: string, training: Training, large?: boolean }) {
    // the session it was sent for, a new workout of the friend can be cheered on again
    const [sentFor, setSentFor] = useState<number | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [pending, startTransition] = useTransition()
    const cheered = training.cheered || sentFor === training.sessionId

    const cheer = () => startTransition(async () => {
        setError(null)
        const result = await cheerFriend(friendId)
        if (result.ok) setSentFor(training.sessionId)
        else setError(result.error)
    })

    return <div className={cn("flex shrink-0 flex-col items-end gap-1", large && "items-stretch")}>
        <button type="button" onClick={cheer} disabled={cheered || pending}
            className={cn("flex items-center justify-center gap-1.5 rounded-full font-semibold transition-all",
                large ? "px-5 py-2.5 text-[17px]" : "px-3.5 py-1.5 text-sm",
                cheered
                    ? "bg-muted text-muted-foreground"
                    : "bg-gradient-to-b from-[#ff9f0a] to-[#ff6b00] text-white shadow-[0_4px_12px_-4px_rgba(255,120,0,.6)] hover:brightness-110 active:scale-95",
                pending && "opacity-70")}>
            {cheered ? <Check className="size-4" /> : <Flame className={cn("size-4", pending && "animate-pulse")} fill="currentColor" />}
            {cheered ? "Angefeuert" : "Anfeuern"}
        </button>
        {error && <span role="alert" className="max-w-56 text-right text-xs text-destructive">{error}</span>}
    </div>
}

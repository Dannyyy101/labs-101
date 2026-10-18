'use client'

import { AvatarImage } from "@/components/AvatarImage"
import { Card } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { Friend } from "@/utils/types/friend"
import { ChevronLeft, UserMinus } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useState, useTransition } from "react"
import { removeFriend } from "../action"
import { CheerButton, TrainingBadge } from "../Training"

// who is training changes while the page is open
const REFRESH_MS = 30_000

/** Name, picture and training of a friend, in place of the own header of the profile page. */
export function FriendHeader({ friend, memberSince }: { friend: Friend, memberSince: string | null }) {
    const router = useRouter()
    const [confirming, setConfirming] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [removing, startRemoving] = useTransition()

    useEffect(() => {
        const timer = setInterval(() => router.refresh(), REFRESH_MS)
        return () => clearInterval(timer)
    }, [router])

    const initial = friend.name?.trim().charAt(0).toUpperCase() || "?"
    const format = (iso: string) => new Date(iso).toLocaleDateString("de-DE", { month: "short", year: "numeric" })

    const remove = () => startRemoving(async () => {
        const result = await removeFriend(friend.id)
        if (result.ok) router.push("/friends")
        else setError(result.error)
    })

    return <div className="flex flex-col gap-5">
        <Link href="/friends" className="-mb-2 flex w-fit items-center text-[17px] text-[#0a84ff] lg:px-1"><ChevronLeft className="size-5" />Freunde</Link>
        <div className="flex flex-row items-center gap-5 lg:flex-col lg:items-start lg:px-1">
            <div className={cn("flex size-24 shrink-0 items-center justify-center overflow-hidden rounded-full bg-gradient-to-b from-muted-foreground/60 to-muted-foreground/90 text-5xl font-semibold text-white lg:size-44 lg:text-7xl",
                friend.training && "ring-4 ring-[#30d158] ring-offset-4 ring-offset-muted")}>
                <AvatarImage src={friend.image} alt="" size={176} className="size-full object-cover" fallback={initial} />
            </div>
            <div className="min-w-0 flex-1 lg:w-full">
                <h1 className="truncate text-[28px] font-bold leading-tight tracking-tight lg:text-[34px]">{friend.name ?? "Unbekannt"}</h1>
                <p className="text-sm text-muted-foreground">Befreundet seit {format(friend.since)}</p>
                {memberSince && <p className="text-sm text-muted-foreground">Mitglied seit {format(memberSince)}</p>}
                {confirming
                    ? <div className="mt-3 flex gap-2 lg:mt-5">
                        <button type="button" onClick={() => setConfirming(false)} className="flex-1 rounded-xl bg-muted py-2 font-medium">Abbrechen</button>
                        <button type="button" onClick={remove} disabled={removing}
                            className="flex-1 rounded-xl bg-destructive/10 py-2 font-medium text-destructive hover:bg-destructive/20 disabled:opacity-60">
                            Entfernen
                        </button>
                    </div>
                    : <button type="button" onClick={() => setConfirming(true)}
                        className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl bg-muted py-2 font-medium transition-colors hover:bg-muted-foreground/15 lg:mt-5">
                        <UserMinus className="size-4" />Freund entfernen
                    </button>}
                {error && <p role="alert" className="mt-1 text-sm text-destructive">{error}</p>}
            </div>
        </div>
        {friend.training && <Card className="flex items-center gap-3 p-5">
            <div className="min-w-0 flex-1">
                <p className="font-semibold">Gerade im Training</p>
                <TrainingBadge training={friend.training} className="mt-0.5" />
            </div>
            <CheerButton friendId={friend.id} training={friend.training} large />
        </Card>}
    </div>
}

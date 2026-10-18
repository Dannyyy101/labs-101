'use client'

import { Card } from "@/components/dashboard"
import { searchUsers } from "@/components/calendar/action"
import { UserAvatar } from "@/components/calendar/UserAvatar"
import { Spinner } from "@/components/ui/spinner"
import { useUser } from "@/components/user-provider"
import { cn } from "@/lib/utils"
import { CalendarUser } from "@/utils/types/calendarTypes"
import { Friend, FriendRequest, Friends } from "@/utils/types/friend"
import { Check, ChevronRight, Search, UserPlus, Users, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useState, useTransition } from "react"
import { acceptFriendRequest, deleteFriendRequest, sendFriendRequest } from "./action"
import { CheerButton, TrainingBadge } from "./Training"

// who is training changes while the page is open
const REFRESH_MS = 30_000

export default function FriendsView({ friends }: { friends: Friends }) {
    const router = useRouter()

    useEffect(() => {
        const timer = setInterval(() => router.refresh(), REFRESH_MS)
        return () => clearInterval(timer)
    }, [router])

    const training = friends.friends.filter((f) => f.training).length

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-2xl flex-col gap-5">
            <div>
                <h1 className="text-[34px] font-bold leading-tight tracking-tight">Freunde</h1>
                {training > 0 && <p className="text-muted-foreground">{training === 1 ? "1 Freund trainiert" : `${training} Freunde trainieren`} gerade</p>}
            </div>

            <AddFriend friends={friends} />

            {friends.incoming.length > 0 && <Section title="Anfragen">
                {friends.incoming.map((request) => <RequestRow key={request.id} request={request} incoming />)}
            </Section>}

            <Section title={`Deine Freunde${friends.friends.length ? ` (${friends.friends.length})` : ""}`}>
                {friends.friends.length
                    ? friends.friends.map((friend) => <FriendRow key={friend.id} friend={friend} />)
                    : <div className="flex flex-col items-center gap-2 px-6 py-10 text-center text-muted-foreground">
                        <Users className="size-8" />
                        <p>Noch keine Freunde. Such oben nach Namen oder E-Mail und schick eine Anfrage.</p>
                    </div>}
            </Section>

            {friends.outgoing.length > 0 && <Section title="Gesendet">
                {friends.outgoing.map((request) => <RequestRow key={request.id} request={request} />)}
            </Section>}
        </div>
    </div>
}

function Section({ title, children }: { title: string, children: React.ReactNode }) {
    return <section>
        <h2 className="px-4 pb-1.5 text-sm font-medium uppercase tracking-wide text-muted-foreground">{title}</h2>
        <Card className="divide-y divide-border/70 p-0">{children}</Card>
    </section>
}

function FriendRow({ friend }: { friend: Friend }) {
    const user: CalendarUser = { id: friend.id, name: friend.name, image: friend.image }

    return <div className="flex items-center gap-3 px-4 py-3">
        <Link href={`/friends/${friend.id}`} className="flex min-w-0 flex-1 items-center gap-3">
            <span className="relative">
                <UserAvatar user={user} className={cn("size-11 text-base ring-0", friend.training && "ring-2 ring-[#30d158] ring-offset-2 ring-offset-card")} />
            </span>
            <span className="min-w-0 flex-1">
                <span className="block truncate text-[17px] font-medium">{friend.name ?? "Unbekannt"}</span>
                {friend.training
                    ? <TrainingBadge training={friend.training} />
                    : <span className="block truncate text-sm text-muted-foreground">Profil & Achievements ansehen</span>}
            </span>
            {!friend.training && <ChevronRight className="size-5 shrink-0 text-muted-foreground/60" />}
        </Link>
        {friend.training && <CheerButton friendId={friend.id} training={friend.training} />}
    </div>
}

function RequestRow({ request, incoming }: { request: FriendRequest, incoming?: boolean }) {
    const [pending, startTransition] = useTransition()
    const [error, setError] = useState<string | null>(null)

    const run = (action: () => Promise<{ ok: boolean, error?: string }>) => startTransition(async () => {
        setError(null)
        const result = await action()
        if (!result.ok) setError(result.error ?? "Das hat nicht geklappt")
    })

    return <div className="flex items-center gap-3 px-4 py-3">
        <UserAvatar user={request.user} className="size-11 text-base ring-0" />
        <div className="min-w-0 flex-1">
            <p className="truncate text-[17px] font-medium">{request.user.name ?? "Unbekannt"}</p>
            <p className="truncate text-sm text-muted-foreground">
                {error ?? (incoming ? "möchte mit dir befreundet sein" : "Anfrage gesendet")}
            </p>
        </div>
        {pending
            ? <Spinner className="size-5" />
            : incoming
                ? <div className="flex gap-2">
                    <button type="button" aria-label="Ablehnen" title="Ablehnen" onClick={() => run(() => deleteFriendRequest(request.id))}
                        className="grid size-9 place-items-center rounded-full bg-muted text-muted-foreground hover:text-[#ff453a]">
                        <X className="size-5" />
                    </button>
                    <button type="button" onClick={() => run(() => acceptFriendRequest(request.id))}
                        className="flex items-center gap-1 rounded-full bg-[#0a84ff] px-3.5 py-1.5 text-sm font-semibold text-white hover:brightness-110">
                        <Check className="size-4" />Annehmen
                    </button>
                </div>
                : <button type="button" onClick={() => run(() => deleteFriendRequest(request.id))}
                    className="rounded-full bg-muted px-3.5 py-1.5 text-sm font-medium text-muted-foreground hover:text-foreground">
                    Zurückziehen
                </button>}
    </div>
}

/** Search people by name or email, everyone already a friend or with an open request is marked. */
function AddFriend({ friends }: { friends: Friends }) {
    const me = useUser()
    const [query, setQuery] = useState("")
    const [results, setResults] = useState<CalendarUser[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        const trimmed = query.trim()
        if (!trimmed) return
        let cancelled = false
        // waits until typing pauses, every key would be a request otherwise
        const timeout = setTimeout(async () => {
            setLoading(true)
            try {
                const users = await searchUsers(trimmed)
                if (!cancelled) setResults(users)
            } catch {
                if (!cancelled) setResults([])
            } finally {
                if (!cancelled) setLoading(false)
            }
        }, 250)
        return () => {
            cancelled = true
            clearTimeout(timeout)
        }
    }, [query])

    const status = new Map<string, string>([
        ...friends.friends.map((f) => [f.id, "Befreundet"] as const),
        ...friends.outgoing.map((r) => [r.user.id, "Angefragt"] as const),
        ...friends.incoming.map((r) => [r.user.id, "Hat dich angefragt"] as const),
    ])
    const options = query.trim() ? results.filter((user) => user.id !== me?.id) : []

    return <div>
        <Card className="p-0">
            <label className="flex items-center gap-3 px-4">
                <Search className="size-5 shrink-0 text-muted-foreground" />
                <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Freunde finden – Name oder E-Mail"
                    className="min-w-0 flex-1 bg-transparent py-3.5 text-[17px] outline-none placeholder:text-muted-foreground" />
                {query && <button type="button" aria-label="Suche leeren" onClick={() => { setQuery(""); setResults([]) }}
                    className="grid size-6 place-items-center rounded-full bg-muted-foreground/30 text-background"><X className="size-3.5" strokeWidth={3} /></button>}
            </label>
            {query.trim() && <div className="divide-y divide-border/70 border-t">
                {loading && options.length === 0
                    ? <div className="flex justify-center p-4"><Spinner className="size-4" /></div>
                    : options.length === 0
                        ? <p className="px-4 py-3.5 text-[15px] text-muted-foreground">Niemand gefunden</p>
                        : options.map((user) => <SearchResult key={user.id} user={user} status={status.get(user.id)} />)}
            </div>}
        </Card>
    </div>
}

function SearchResult({ user, status }: { user: CalendarUser, status?: string }) {
    const [pending, startTransition] = useTransition()
    const [error, setError] = useState<string | null>(null)

    const send = () => startTransition(async () => {
        setError(null)
        const result = await sendFriendRequest(user.id)
        if (!result.ok) setError(result.error)
    })

    return <div className="flex items-center gap-3 px-4 py-2.5">
        <UserAvatar user={user} className="size-9 text-sm ring-0" />
        <div className="min-w-0 flex-1">
            <p className="truncate text-[17px]">{user.name ?? "Unbekannt"}</p>
            {error && <p role="alert" className="truncate text-sm text-destructive">{error}</p>}
        </div>
        {status
            ? <span className="text-sm text-muted-foreground">{status}</span>
            : <button type="button" onClick={send} disabled={pending}
                className="flex items-center gap-1.5 rounded-full bg-[#0a84ff] px-3.5 py-1.5 text-sm font-semibold text-white hover:brightness-110 disabled:opacity-60">
                {pending ? <Spinner className="size-4" /> : <UserPlus className="size-4" />}Hinzufügen
            </button>}
    </div>
}

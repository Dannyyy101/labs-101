'use client'
import { useEffect, useState } from "react";
import { Plus, X } from "lucide-react";
import { Spinner } from "../ui/spinner";
import { CalendarUser } from "@/utils/types/calendarTypes";
import { searchUsers } from "./action";
import { UserAvatar } from "./UserAvatar";
import { Group } from "./Group";

/** Search for people by name or email and collect them as invitees. */
export function InviteField({ invitees, setInvitees, excludeIds }: { invitees: CalendarUser[], setInvitees: (invitees: CalendarUser[]) => void, excludeIds: string[] }) {
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

    const hidden = new Set([...excludeIds, ...invitees.map((user) => user.id)])
    const options = query.trim() ? results.filter((user) => !hidden.has(user.id)) : []

    const invite = (user: CalendarUser) => {
        setInvitees([...invitees, user])
        setQuery("")
        setResults([])
    }

    return <div className="relative">
        <Group>
            {invitees.map((user) =>
                <div key={user.id} className="flex min-h-11 items-center gap-3 px-4 py-1.5">
                    <UserAvatar user={user} className="size-7 text-xs ring-0" />
                    <span className="flex-1 truncate text-[17px]">{user.name}</span>
                    <button type="button" aria-label={`${user.name} entfernen`} title="Entfernen"
                        className="grid size-7 place-items-center rounded-full text-muted-foreground hover:bg-muted hover:text-[#ff453a]"
                        onClick={() => setInvitees(invitees.filter((u) => u.id !== user.id))}>
                        <X className="size-4" />
                    </button>
                </div>
            )}
            <label className="flex min-h-11 items-center gap-3 px-4">
                <span className="grid size-7 place-items-center rounded-full bg-[#0a84ff] text-white"><Plus className="size-4" /></span>
                <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Person hinzufügen"
                    className="flex-1 bg-transparent py-2.5 text-[17px] outline-none placeholder:text-[#0a84ff]"
                    // enter picks the first match instead of submitting the form
                    onKeyDown={(e) => {
                        if (e.key === "Enter") {
                            e.preventDefault()
                            if (options[0]) invite(options[0])
                        }
                    }} />
            </label>
        </Group>
        {query.trim() && <Group className="mt-2 shadow-lg ring-1 ring-foreground/5">
            {loading && options.length === 0
                ? <div className="flex justify-center p-3"><Spinner className="size-4" /></div>
                : options.length === 0
                    ? <p className="px-4 py-3 text-[15px] text-muted-foreground">Niemand gefunden</p>
                    : options.map((user) =>
                        <button type="button" key={user.id} onClick={() => invite(user)}
                            className="flex min-h-11 w-full items-center gap-3 px-4 py-1.5 text-left text-[17px] hover:bg-muted/60">
                            <UserAvatar user={user} className="size-7 text-xs ring-0" />
                            {user.name}
                        </button>
                    )}
        </Group>}
    </div>
}

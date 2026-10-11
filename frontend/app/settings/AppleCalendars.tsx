'use client'

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { ExternalCalendar } from "@/utils/types/calendarTypes"
import { AlertCircle, CalendarDays, Link2, Trash2 } from "lucide-react"
import { useState, useTransition } from "react"
import { addExternalCalendar, connectAppleAccount, deleteExternalCalendar, disconnectAppleAccount } from "./calendarActions"

/** Apple calendars, of a connected Apple ID or shared by link. Their events are shown read only in the planner. */
export default function AppleCalendars({ calendars: initial }: { calendars: ExternalCalendar[] }) {
    const [calendars, setCalendars] = useState(initial)
    const [error, setError] = useState<string | null>(null)
    const [pending, run] = useTransition()

    // the calendars by their Apple ID, the shared ones under null
    const accounts = new Map<number, { appleId: string, calendars: ExternalCalendar[] }>()
    for (const calendar of calendars) {
        if (calendar.accountId == null) continue
        const account = accounts.get(calendar.accountId) ?? { appleId: calendar.appleId ?? "", calendars: [] }
        account.calendars.push(calendar)
        accounts.set(calendar.accountId, account)
    }
    const shared = calendars.filter((c) => c.accountId == null)

    const removeCalendar = (calendar: ExternalCalendar) => {
        if (!confirm(`„${calendar.name}“ nicht mehr im Planer anzeigen?`)) return
        run(async () => {
            setError(null)
            const result = await deleteExternalCalendar(calendar.id)
            if (result.ok) setCalendars((prev) => prev.filter((c) => c.id !== calendar.id))
            else setError(result.error)
        })
    }

    const disconnect = (accountId: number, appleId: string) => {
        if (!confirm(`${appleId} trennen? Die Kalender werden nicht mehr im Planer angezeigt.`)) return
        run(async () => {
            setError(null)
            const result = await disconnectAppleAccount(accountId)
            if (result.ok) setCalendars((prev) => prev.filter((c) => c.accountId !== accountId))
            else setError(result.error)
        })
    }

    return <div className="rounded-3xl bg-card shadow-sm p-6 flex flex-col gap-5">
        <div className="flex flex-col gap-1">
            <h2 className="text-xl font-semibold">Apple-Kalender</h2>
            <p className="text-sm text-muted-foreground">
                Termine aus deinen Apple-Kalendern werden im Planer angezeigt, ändern kannst du sie weiter in Apple Kalender.
            </p>
        </div>

        {[...accounts.entries()].map(([accountId, account]) =>
            <section key={accountId} className="flex flex-col gap-2">
                <div className="flex items-center justify-between gap-3">
                    <p className="truncate text-sm font-medium">{account.appleId}</p>
                    <Button type="button" variant="ghost" size="sm" disabled={pending} onClick={() => disconnect(accountId, account.appleId)}>
                        Trennen
                    </Button>
                </div>
                <CalendarList calendars={account.calendars} pending={pending} remove={removeCalendar} />
            </section>)}

        {shared.length > 0 && <section className="flex flex-col gap-2">
            <p className="text-sm font-medium">Über Link</p>
            <CalendarList calendars={shared} pending={pending} remove={removeCalendar} />
        </section>}

        {error && (
            <Alert variant="destructive">
                <AlertCircle className="size-4" />
                <AlertTitle>Fehler</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        )}

        <ConnectForm pending={pending} run={run} setError={setError} onConnected={setCalendars} />

        <details className="group">
            <summary className="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
                <Link2 className="size-4" />Oder einen öffentlich freigegebenen Kalender über seinen Link hinzufügen
            </summary>
            <LinkForm pending={pending} run={run} setError={setError}
                onAdded={(calendar) => setCalendars((prev) => [...prev, calendar])} />
        </details>
    </div>
}

function CalendarList({ calendars, pending, remove }: { calendars: ExternalCalendar[], pending: boolean, remove: (calendar: ExternalCalendar) => void }) {
    return <ul className="divide-y divide-border/70 rounded-xl border">
        {calendars.map((calendar) => <li key={calendar.id} className="flex items-center gap-3 px-4 py-2">
            <CalendarDays className="size-4 shrink-0 text-muted-foreground" />
            <div className="min-w-0 flex-1">
                <p className="truncate">{calendar.name}</p>
                {calendar.error && <p className="text-xs text-destructive">{calendar.error}</p>}
            </div>
            <Button type="button" variant="ghost" size="icon" disabled={pending} onClick={() => remove(calendar)}
                aria-label={`${calendar.name} ausblenden`}>
                <Trash2 className="size-4" />
            </Button>
        </li>)}
    </ul>
}

type FormProps = {
    pending: boolean
    run: (action: () => Promise<void>) => void
    setError: (error: string | null) => void
}

function ConnectForm({ pending, run, setError, onConnected }: FormProps & { onConnected: (calendars: ExternalCalendar[]) => void }) {
    const [appleId, setAppleId] = useState("")
    const [password, setPassword] = useState("")

    const connect = () => run(async () => {
        setError(null)
        const result = await connectAppleAccount({ appleId, password })
        if (result.ok) {
            onConnected(result.data)
            setPassword("")
        } else {
            setError(result.error)
        }
    })

    return <form className="flex flex-col gap-4" onSubmit={(e) => { e.preventDefault(); if (appleId.trim() && password.trim()) connect() }}>
        <Field>
            <FieldLabel htmlFor="apple-id">Apple-ID</FieldLabel>
            <Input id="apple-id" type="email" autoComplete="username" value={appleId} onChange={(e) => setAppleId(e.target.value)}
                placeholder="name@icloud.com" className="max-w-80" />
        </Field>
        <Field>
            <FieldLabel htmlFor="apple-password">App-spezifisches Passwort</FieldLabel>
            <Input id="apple-password" type="password" autoComplete="off" value={password} onChange={(e) => setPassword(e.target.value)}
                placeholder="xxxx-xxxx-xxxx-xxxx" className="max-w-80" />
            <FieldDescription>
                Nicht dein Apple-ID-Passwort: Erstelle auf{" "}
                <a href="https://account.apple.com/account/manage" target="_blank" rel="noreferrer" className="underline">account.apple.com</a>{" "}
                unter „Anmelden und Sicherheit“ → „App-spezifische Passwörter“ ein neues, z. B. „Labs 101“.
                Dort kannst du es auch jederzeit widerrufen. Labs speichert es verschlüsselt und liest damit nur deine Kalender.
            </FieldDescription>
        </Field>
        <div>
            <Button type="submit" disabled={!appleId.trim() || !password.trim() || pending}>
                {pending && <Spinner />}Mit Apple verbinden
            </Button>
        </div>
    </form>
}

function LinkForm({ pending, run, setError, onAdded }: FormProps & { onAdded: (calendar: ExternalCalendar) => void }) {
    const [name, setName] = useState("")
    const [url, setUrl] = useState("")

    const add = () => run(async () => {
        setError(null)
        const result = await addExternalCalendar({ name, url })
        if (result.ok) {
            onAdded(result.data)
            setName("")
            setUrl("")
        } else {
            setError(result.error)
        }
    })

    return <form className="mt-4 flex flex-col gap-4" onSubmit={(e) => { e.preventDefault(); if (url.trim()) add() }}>
        <Field>
            <FieldLabel htmlFor="calendar-url">Link des Kalenders</FieldLabel>
            <Input id="calendar-url" value={url} onChange={(e) => setUrl(e.target.value)}
                placeholder="webcal://p…-caldav.icloud.com/published/2/…" autoComplete="off" />
            <FieldDescription>
                In Apple Kalender: Rechtsklick auf den Kalender → „Kalender teilen …“ → „Öffentlicher Kalender“.
                Wer den Link hat, kann den Kalender lesen.
            </FieldDescription>
        </Field>
        <Field>
            <FieldLabel htmlFor="calendar-name">Name (optional)</FieldLabel>
            <Input id="calendar-name" value={name} onChange={(e) => setName(e.target.value)}
                placeholder="Name aus Apple Kalender" className="max-w-80" />
        </Field>
        <div>
            <Button type="submit" variant="secondary" disabled={!url.trim() || pending}>
                {pending && <Spinner />}Kalender hinzufügen
            </Button>
        </div>
    </form>
}

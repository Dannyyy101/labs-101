'use client'

import { Card, CardHead, Stat } from "@/components/dashboard"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import { SessionExercise, SessionSet, WorkoutSession } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { Check, ChevronLeft, Plus, Trash2, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useCallback, useEffect, useMemo, useRef, useState, useTransition } from "react"
import { formatDuration } from "../../runs/format"
import { deleteSession, finishSession, updateSession } from "../action"
import ExercisePicker from "../ExercisePicker"
import NumberInput from "../NumberInput"
import MuscleMap from "../MuscleMap"
import { COLORS, formatSet, formatVolume, formatWeight, lastSets, MUSCLES, sessionDuration, setsPerMuscle, volume } from "../stats"
import { useNow } from "../useNow"

const REST_SECONDS = 90
const SAVE_DELAY = 700

type SaveState = "saved" | "pending" | "saving" | "error"

export default function SessionView({ session, exercises, history }: { session: WorkoutSession, exercises: Exercise[], history: WorkoutSession[] }) {
    const router = useRouter()
    const now = useNow()
    const [name, setName] = useState(session.name)
    const [items, setItems] = useState<SessionExercise[]>(session.exercises)
    const [rest, setRest] = useState<{ until: number, total: number } | null>(null)
    const [finishing, setFinishing] = useState(false)

    const exerciseById = useMemo(() => new Map(exercises.map((e) => [e.id, e])), [exercises])
    const sets = items.flatMap((e) => e.sets)
    const done = sets.filter((s) => s.done)
    const [vol, volUnit] = formatVolume(volume(done))
    const muscles = useMemo(() => setsPerMuscle(items.map((e) => ({ exerciseId: e.exerciseId, sets: e.sets.filter((s) => s.done).length })), exerciseById), [items, exerciseById])

    const { state: saveState, retry, cancel } = useAutosave(session.id, name, items)

    const update = (index: number, item: SessionExercise) => setItems((prev) => prev.map((e, i) => i === index ? item : e))
    const add = (exercise: Exercise) => setItems((prev) => {
        const last = lastSets(history, exercise.id)
        const fresh: SessionSet = { reps: 10, weightKg: 0, rpe: null, done: false }
        const template = last?.length ? last : [fresh, fresh, fresh]
        return [...prev, { exerciseId: exercise.id, name: exercise.name, sets: template.map((s) => ({ ...s, done: false })) }]
    })
    const toggled = (set: SessionSet) => {
        if (set.done) setRest({ until: Date.now() + REST_SECONDS * 1000, total: REST_SECONDS })
    }

    return <div className="flex-1 w-full bg-muted/50 px-3.5 pb-36 pt-5 md:px-6 md:pt-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-64 flex-1">
                    <Link href="/workouts" className="flex w-fit items-center text-[15px] text-[#0a84ff]"><ChevronLeft className="size-5" />Training</Link>
                    <input value={name} onChange={(e) => setName(e.target.value)} aria-label="Name"
                        className="mt-1 w-full bg-transparent text-[34px] font-bold leading-tight tracking-tight outline-none" />
                    <p className="flex items-center gap-2 text-muted-foreground">
                        <span className="relative flex size-2.5">
                            <span className="absolute inline-flex size-full animate-ping rounded-full bg-[#30d158] opacity-75" />
                            <span className="relative inline-flex size-2.5 rounded-full bg-[#30d158]" />
                        </span>
                        Läuft seit {new Date(session.startedAt).toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })} Uhr
                        <span className="text-sm">· <SaveStatus state={saveState} onRetry={retry} /></span>
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <div className="flex flex-col gap-4 lg:col-span-8">
                    {items.map((item, index) => <ExerciseLog key={index} item={item} exercise={item.exerciseId != null ? exerciseById.get(item.exerciseId) : undefined}
                        previous={lastSets(history, item.exerciseId)}
                        onChange={(next) => update(index, next)} onToggle={toggled}
                        onRemove={() => {
                            if (!item.sets.some((s) => s.done) || confirm(`${item.name} mit allen Sätzen entfernen?`))
                                setItems((prev) => prev.filter((_, i) => i !== index))
                        }} />)}
                    {!items.length && <Card className="py-10 text-center">
                        <p className="text-lg font-semibold">Leeres Training</p>
                        <p className="mt-1 text-muted-foreground">Füge die erste Übung hinzu, um Sätze zu erfassen.</p>
                    </Card>}
                    <ExercisePicker exercises={exercises} onPick={add}
                        className="flex items-center justify-center gap-2 rounded-[18px] border-2 border-dashed p-5 font-semibold text-[#0a84ff] hover:bg-card">
                        <Plus className="size-5" />Übung hinzufügen
                    </ExercisePicker>
                    <DiscardButton session={session} />
                </div>

                <div className="flex flex-col gap-4 lg:col-span-4">
                    <Card>
                        <CardHead title="Fortschritt" right={`${done.length} / ${sets.length} Sätze`} />
                        <div className="mb-4 h-2.5 overflow-hidden rounded-full bg-muted">
                            <div className="h-full rounded-full bg-[#30d158] transition-[width]" style={{ width: `${sets.length ? done.length / sets.length * 100 : 0}%` }} />
                        </div>
                        <dl className="grid grid-cols-3 gap-3">
                            <Stat label="Zeit" value={formatDuration(sessionDuration(session, now))} />
                            <Stat label="Sätze" value={`${done.length}`} />
                            <Stat label="Volumen" value={vol} unit={volUnit} />
                        </dl>
                    </Card>
                    <Card className="hidden lg:block">
                        <CardHead title="Muskeln" right="Abgehakte Sätze" />
                        <MuscleMap muscles={muscles} legend={4} />
                    </Card>
                </div>
            </div>
        </div>

        <BottomBar elapsed={sessionDuration(session, now)} now={now} rest={rest} setRest={setRest}
            progress={`${done.length} / ${sets.length} Sätze`} onFinish={() => setFinishing(true)} />

        <FinishDialog open={finishing} onOpenChange={setFinishing} session={session} name={name} items={items}
            elapsed={sessionDuration(session, now)} beforeFinish={cancel}
            onFinished={() => {
                router.push("/workouts")
                router.refresh()
            }} />
    </div>
}

/** Saves name and exercises shortly after every change and when leaving the page, `cancel` drops a pending save. */
function useAutosave(id: number, name: string, items: SessionExercise[]) {
    const [state, setState] = useState<SaveState>("saved")
    const latest = useRef({ name, items })
    const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
    const first = useRef(true)

    const save = useCallback(async () => {
        if (timer.current) clearTimeout(timer.current)
        timer.current = null
        const { name, items } = latest.current
        setState("saving")
        try {
            await updateSession(id, name, items)
            // only "saved" when nothing changed in the meantime
            if (latest.current.items === items && latest.current.name === name) setState("saved")
        } catch {
            setState("error")
        }
    }, [id])

    useEffect(() => {
        latest.current = { name, items }
        if (first.current) {
            first.current = false
            return
        }
        setState("pending")
        if (timer.current) clearTimeout(timer.current)
        timer.current = setTimeout(save, SAVE_DELAY)
    }, [name, items, save])

    // navigating away saves right away, closing the tab asks first
    useEffect(() => {
        const warn = (e: BeforeUnloadEvent) => { if (timer.current) e.preventDefault() }
        window.addEventListener("beforeunload", warn)
        return () => {
            window.removeEventListener("beforeunload", warn)
            if (timer.current) save()
        }
    }, [save])

    const cancel = useCallback(() => {
        if (timer.current) clearTimeout(timer.current)
        timer.current = null
    }, [])
    return { state, retry: save, cancel }
}

function SaveStatus({ state, onRetry }: { state: SaveState, onRetry: () => void }) {
    switch (state) {
        case "saved": return <span>Gespeichert</span>
        case "pending":
        case "saving": return <span>Speichert…</span>
        case "error": return <button onClick={onRetry} className="font-medium text-[#ff453a]">Nicht gespeichert – erneut versuchen</button>
    }
}

// MARK: exercises

function ExerciseLog({ item, exercise, previous, onChange, onToggle, onRemove }: {
    item: SessionExercise, exercise?: Exercise, previous: SessionSet[] | null,
    onChange: (item: SessionExercise) => void, onToggle: (set: SessionSet) => void, onRemove: () => void
}) {
    const muscles = [...new Set(exercise?.bodyParts.map((p) => p.slug).filter(Boolean) ?? [])]
    const finished = item.sets.length > 0 && item.sets.every((s) => s.done)
    const updateSet = (index: number, set: Partial<SessionSet>) =>
        onChange({ ...item, sets: item.sets.map((s, i) => i === index ? { ...s, ...set } : s) })
    const addSet = () => {
        const last = item.sets[item.sets.length - 1]
        onChange({ ...item, sets: [...item.sets, { reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0, rpe: null, done: false }] })
    }

    return <Card className={cn("transition-shadow", finished && "ring-2 ring-[#30d158]/40")}>
        <div className="mb-3 flex items-start gap-2">
            <div className="min-w-0 flex-1">
                <h2 className="flex items-center gap-2 truncate text-[17px] font-semibold">
                    {item.name}
                    {finished && <span className="grid size-5 place-items-center rounded-full bg-[#30d158] text-white"><Check className="size-3.5" strokeWidth={3} /></span>}
                </h2>
                <p className="truncate text-sm text-muted-foreground">
                    {previous ? `Letztes Mal: ${previous.map(formatSet).join(", ")}` : muscles.map((m) => MUSCLES[m!] ?? m).join(", ") || "Zum ersten Mal"}
                </p>
            </div>
            <button type="button" aria-label="Übung entfernen" title="Übung entfernen" onClick={onRemove}
                className="grid size-8 flex-none place-items-center rounded-full text-[#ff453a] hover:bg-muted"><Trash2 className="size-4" /></button>
        </div>
        <div className="grid grid-cols-[28px_minmax(0,0.9fr)_minmax(0,1fr)_minmax(0,1fr)_40px_24px] items-center gap-x-2 gap-y-1.5 md:gap-x-3">
            <span className="text-center text-xs font-medium text-muted-foreground">Satz</span>
            <span className="text-xs font-medium text-muted-foreground">Vorher</span>
            <span className="text-xs font-medium text-muted-foreground">kg</span>
            <span className="text-xs font-medium text-muted-foreground">Wdh</span>
            <span />
            <span />
            {item.sets.map((set, i) => {
                const before = previous?.[i]
                return <div key={i} className="contents">
                    <span className={cn("text-center font-semibold tabular-nums", set.done ? "text-[#30d158]" : "text-muted-foreground")}>{i + 1}</span>
                    <button type="button" disabled={!before} onClick={() => before && updateSet(i, { reps: before.reps, weightKg: before.weightKg })}
                        title={before ? "Übernehmen" : undefined}
                        className="truncate text-left text-sm tabular-nums text-muted-foreground enabled:hover:text-foreground">
                        {before ? `${formatWeight(before.weightKg)} × ${before.reps}` : "–"}
                    </button>
                    <NumberInput value={set.weightKg} onChange={(weightKg) => updateSet(i, { weightKg })} label={`Gewicht Satz ${i + 1}`} decimal
                        className={cn(set.done && "bg-[#30d158]/12")} />
                    <NumberInput value={set.reps} onChange={(reps) => updateSet(i, { reps: Math.round(reps) })} label={`Wiederholungen Satz ${i + 1}`}
                        className={cn(set.done && "bg-[#30d158]/12")} />
                    <button type="button" aria-label={set.done ? `Satz ${i + 1} nicht erledigt` : `Satz ${i + 1} erledigt`} aria-pressed={set.done}
                        onClick={() => {
                            const next = { ...set, done: !set.done }
                            updateSet(i, next)
                            onToggle(next)
                        }}
                        className={cn("grid h-9 w-10 place-items-center rounded-[10px] transition-colors",
                            set.done ? "bg-[#30d158] text-white" : "bg-muted text-muted-foreground hover:text-foreground")}>
                        <Check className="size-5" strokeWidth={3} />
                    </button>
                    <button type="button" aria-label={`Satz ${i + 1} entfernen`} onClick={() => onChange({ ...item, sets: item.sets.filter((_, j) => j !== i) })}
                        className="grid size-6 place-items-center rounded-full text-muted-foreground/60 hover:text-[#ff453a]"><X className="size-3.5" /></button>
                </div>
            })}
        </div>
        <button onClick={addSet} className="mt-3 flex items-center gap-1 text-sm font-medium text-[#0a84ff]"><Plus className="size-4" />Satz hinzufügen</button>
    </Card>
}

// MARK: bottom bar

function BottomBar({ elapsed, now, rest, setRest, progress, onFinish }: {
    elapsed: number, now: number, rest: { until: number, total: number } | null,
    setRest: (rest: { until: number, total: number } | null) => void, progress: string, onFinish: () => void
}) {
    const left = rest ? Math.ceil((rest.until - now) / 1000) : 0
    const over = rest != null && left <= 0
    const vibrated = useRef<number | null>(null)

    useEffect(() => {
        if (!rest || !over || vibrated.current === rest.until) return
        vibrated.current = rest.until
        navigator.vibrate?.([200, 100, 200])
        // the hint disappears a few seconds after the rest is over
        const timer = setTimeout(() => setRest(null), 5000)
        return () => clearTimeout(timer)
    }, [rest, over, setRest])

    const adjust = (by: number) => rest && setRest({ until: Math.max(Date.now(), rest.until + by * 1000), total: Math.max(15, rest.total + by) })

    return <div className="fixed inset-x-0 bottom-0 z-30 px-3 pb-[max(12px,env(safe-area-inset-bottom))]">
        <div className="mx-auto flex max-w-3xl flex-col gap-2 rounded-[22px] bg-background/85 p-2.5 shadow-[0_8px_32px_rgba(0,0,0,.18)] ring-1 ring-foreground/5 backdrop-blur-xl">
            {rest && <div className="relative flex items-center gap-2 overflow-hidden rounded-[14px] px-3 py-2" style={{ background: `${over ? COLORS.sets : COLORS.volume}22` }}>
                {!over && <div className="absolute inset-y-0 left-0 transition-[width] duration-1000 ease-linear" style={{ width: `${left / rest.total * 100}%`, background: `${COLORS.volume}22` }} />}
                <span className="relative flex-1 font-semibold" style={{ color: over ? "#248a3d" : "#c76f00" }}>
                    {over ? "Pause vorbei – nächster Satz!" : <>Pause <span className="tabular-nums">{formatDuration(left)}</span></>}
                </span>
                {!over && <>
                    <button onClick={() => adjust(-15)} className="relative rounded-full bg-background/70 px-2.5 py-1 text-sm font-semibold tabular-nums">−15</button>
                    <button onClick={() => adjust(15)} className="relative rounded-full bg-background/70 px-2.5 py-1 text-sm font-semibold tabular-nums">+15</button>
                </>}
                <button onClick={() => setRest(null)} aria-label="Pause beenden" className="relative grid size-7 place-items-center rounded-full bg-background/70"><X className="size-4" /></button>
            </div>}
            <div className="flex items-center gap-3 pl-2">
                <div className="min-w-0 flex-1">
                    <div className="text-2xl font-bold leading-tight tabular-nums">{formatDuration(elapsed)}</div>
                    <div className="text-xs text-muted-foreground">{progress}</div>
                </div>
                <button onClick={onFinish} className="rounded-full bg-[#30d158] px-5 py-2.5 font-semibold text-white">Beenden</button>
            </div>
        </div>
    </div>
}

// MARK: finish

function FinishDialog({ open, onOpenChange, session, name, items, elapsed, beforeFinish, onFinished }: {
    open: boolean, onOpenChange: (open: boolean) => void, session: WorkoutSession, name: string, items: SessionExercise[],
    elapsed: number, beforeFinish: () => void, onFinished: () => void
}) {
    const [pending, start] = useTransition()
    const [error, setError] = useState(false)
    const sets = items.flatMap((e) => e.sets)
    const done = sets.filter((s) => s.done)
    const [vol, volUnit] = formatVolume(volume(done))
    const open_ = sets.length - done.length

    const finish = () => start(async () => {
        setError(false)
        // the finish request carries the latest state, a save racing it would be rejected anyway
        beforeFinish()
        try {
            await finishSession(session.id, name, items)
            onFinished()
        } catch {
            setError(true)
        }
    })
    const discard = () => start(async () => {
        await deleteSession(session.id)
        onFinished()
    })

    return <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="gap-5">
            <DialogHeader>
                <DialogTitle className="text-xl">{done.length ? "Training beenden?" : "Noch kein Satz erledigt"}</DialogTitle>
                <DialogDescription>
                    {done.length
                        ? open_ > 0 ? `${open_} ${open_ === 1 ? "Satz ist" : "Sätze sind"} nicht abgehakt und ${open_ === 1 ? "wird" : "werden"} nicht gespeichert.` : "Starke Leistung, alle Sätze erledigt!"
                        : "Hake erledigte Sätze ab, damit sie gespeichert werden – oder verwirf das Training."}
                </DialogDescription>
            </DialogHeader>
            <dl className="grid grid-cols-3 gap-3 rounded-[14px] bg-muted/60 p-4">
                <Stat label="Zeit" value={formatDuration(elapsed)} />
                <Stat label="Sätze" value={`${done.length}`} />
                <Stat label="Volumen" value={vol} unit={volUnit} />
            </dl>
            {error && <p className="text-sm font-medium text-[#ff453a]">Beenden fehlgeschlagen, versuche es noch einmal.</p>}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button onClick={() => onOpenChange(false)} disabled={pending} className="rounded-full bg-muted px-4 py-2.5 font-semibold disabled:opacity-50">Weiter trainieren</button>
                {done.length
                    ? <button onClick={finish} disabled={pending} className="rounded-full bg-[#30d158] px-5 py-2.5 font-semibold text-white disabled:opacity-50">
                        {pending ? "Speichert…" : "Training beenden"}
                    </button>
                    : <button onClick={discard} disabled={pending} className="rounded-full bg-[#ff453a] px-5 py-2.5 font-semibold text-white disabled:opacity-50">
                        {pending ? "Verwirft…" : "Training verwerfen"}
                    </button>}
            </div>
        </DialogContent>
    </Dialog>
}

function DiscardButton({ session }: { session: WorkoutSession }) {
    const router = useRouter()
    const [pending, start] = useTransition()
    const discard = () => {
        if (!confirm("Training verwerfen? Alle erfassten Sätze gehen verloren.")) return
        start(async () => {
            await deleteSession(session.id)
            router.push("/workouts")
            router.refresh()
        })
    }
    return <button onClick={discard} disabled={pending} className="self-center px-3 py-1.5 text-sm font-medium text-[#ff453a] disabled:opacity-50">
        {pending ? "Verwirft…" : "Training verwerfen"}
    </button>
}

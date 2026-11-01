'use client'

import { Card, CardHead, Stat } from "@/components/dashboard"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import { SessionExercise, SessionSet, WorkoutSession } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { Check, ChevronLeft, HeartPulse, Plus, StickyNote, Trash2, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore, useTransition } from "react"
import { formatDuration } from "../../runs/format"
import { deleteSession, finishSession, updateSession } from "../action"
import ExercisePicker from "../ExercisePicker"
import NumberInput from "../NumberInput"
import RepsPicker from "../RepsPicker"
import RestPicker from "../RestPicker"
import MuscleMap from "../MuscleMap"
import { COLORS, DEFAULT_CARDIO_MINUTES, DEFAULT_REST, formatSet, formatVolume, formatWeight, isCardio, lastCardioMinutes, lastNote, lastSets, MUSCLES, restOf, sessionDuration, setsPerMuscle, volume } from "../stats"
import { useNow } from "../useNow"

const SAVE_DELAY = 700

type SaveState = "saved" | "pending" | "saving" | "error"

export default function SessionView({ session, exercises, history }: { session: WorkoutSession, exercises: Exercise[], history: WorkoutSession[] }) {
    const router = useRouter()
    const now = useNow()
    const [name, setName] = useState(session.name)
    const [items, setItems] = useState<SessionExercise[]>(session.exercises)
    const [rest, setRest] = useStoredRest(session.id)
    const [finishing, setFinishing] = useState(false)

    const exerciseById = useMemo(() => new Map(exercises.map((e) => [e.id, e])), [exercises])
    const strengthExercises = useMemo(() => exercises.filter((e) => !isCardio(e)), [exercises])
    const cardioExercises = useMemo(() => exercises.filter(isCardio), [exercises])
    const sets = items.flatMap((e) => e.sets)
    const done = sets.filter((s) => s.done)
    const [vol, volUnit] = formatVolume(volume(done))
    const muscles = useMemo(() => setsPerMuscle(items.map((e) => ({ exerciseId: e.exerciseId, sets: e.sets.filter((s) => s.done).length })), exerciseById), [items, exerciseById])

    const { state: saveState, retry, cancel } = useAutosave(session.id, name, items)

    const update = (index: number, item: SessionExercise) => setItems((prev) => prev.map((e, i) => i === index ? item : e))
    const add = (exercise: Exercise) => setItems((prev) => {
        const last = lastSets(history, exercise.id)
        const fresh: SessionSet = { reps: 10, weightKg: 0, rpe: null, done: false, restSeconds: DEFAULT_REST }
        const template = last?.length ? last : [fresh, fresh, fresh]
        return [...prev, { exerciseId: exercise.id, name: exercise.name, note: lastNote(history, exercise.id), sets: template.map((s) => ({ ...s, done: false })), cardio: null }]
    })
    // with the minutes of last time
    const addCardio = (exercise: Exercise) => setItems((prev) => [...prev, {
        exerciseId: exercise.id, name: exercise.name, note: lastNote(history, exercise.id), sets: [],
        cardio: { minutes: lastCardioMinutes(history, exercise.id) ?? DEFAULT_CARDIO_MINUTES, done: false },
    }])
    // checking a set off starts its rest
    const toggled = (set: SessionSet) => {
        const seconds = restOf(set)
        if (set.done && seconds > 0) setRest({ until: Date.now() + seconds * 1000, total: seconds })
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
                        {saveState === "error" && <span className="text-sm">· <button onClick={retry} className="font-medium text-[#ff453a]">Nicht gespeichert – erneut versuchen</button></span>}
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <div className="flex flex-col gap-4 lg:col-span-8">
                    {items.map((item, index) => item.cardio
                        ? <CardioLog key={index} item={item} previous={lastCardioMinutes(history, item.exerciseId)}
                            onChange={(next) => update(index, next)}
                            onRemove={() => setItems((prev) => prev.filter((_, i) => i !== index))} />
                        : <ExerciseLog key={index} item={item} exercise={item.exerciseId != null ? exerciseById.get(item.exerciseId) : undefined}
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
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <ExercisePicker exercises={strengthExercises} onPick={add}
                            className="flex items-center justify-center gap-2 rounded-[18px] border-2 border-dashed p-5 font-semibold text-[#0a84ff] hover:bg-card">
                            <Plus className="size-5" />Übung hinzufügen
                        </ExercisePicker>
                        <ExercisePicker exercises={cardioExercises} onPick={addCardio} title="Cardio hinzufügen"
                            className="flex items-center justify-center gap-2 rounded-[18px] border-2 border-dashed p-5 font-semibold text-[#ff375f] hover:bg-card">
                            <HeartPulse className="size-5" />Cardio hinzufügen
                        </ExercisePicker>
                    </div>
                    <button onClick={() => setFinishing(true)} className="rounded-full bg-[#30d158] px-5 py-3 font-semibold text-white">Training beenden</button>
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
            progress={`${done.length} / ${sets.length} Sätze`} />

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

type Rest = { until: number, total: number }

/** The running rest, kept in the browser so it survives leaving and reopening the workout. */
function useStoredRest(id: number) {
    const key = `workout-rest-${id}`
    const subscribe = useCallback((onChange: () => void) => {
        window.addEventListener(REST_EVENT, onChange)
        window.addEventListener("storage", onChange)
        return () => {
            window.removeEventListener(REST_EVENT, onChange)
            window.removeEventListener("storage", onChange)
        }
    }, [])
    const stored = useSyncExternalStore(subscribe, () => readStorage(key), () => null)
    const rest = useMemo(() => stored ? JSON.parse(stored) as Rest : null, [stored])

    const update = useCallback((next: Rest | null) => {
        try {
            if (next) localStorage.setItem(key, JSON.stringify(next))
            else localStorage.removeItem(key)
        } catch { }
        window.dispatchEvent(new Event(REST_EVENT))
    }, [key])
    return [rest, update] as const
}

const REST_EVENT = "workout-rest"

function readStorage(key: string) {
    try {
        const stored = localStorage.getItem(key)
        // a rest that is long over isn't worth showing anymore
        if (stored && (JSON.parse(stored) as Rest).until < Date.now() - 5000) {
            localStorage.removeItem(key)
            return null
        }
        return stored
    } catch {
        return null
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
        onChange({ ...item, sets: [...item.sets, { reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0, rpe: null, done: false, restSeconds: last?.restSeconds ?? DEFAULT_REST }] })
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
        <NoteField note={item.note} onChange={(note) => onChange({ ...item, note })} />
        <div className="grid grid-cols-[24px_minmax(0,0.9fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,0.9fr)_40px_24px] items-center gap-x-2 gap-y-1.5 md:gap-x-3">
            <span className="text-center text-xs font-medium text-muted-foreground">Satz</span>
            <span className="text-xs font-medium text-muted-foreground">Vorher</span>
            <span className="text-xs font-medium text-muted-foreground">kg</span>
            <span className="text-xs font-medium text-muted-foreground">Wdh</span>
            <span className="text-xs font-medium text-muted-foreground">Pause</span>
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
                    <RepsPicker value={set.reps} onChange={(reps) => updateSet(i, { reps })} label={`Wiederholungen Satz ${i + 1}`}
                        className={cn(set.done && "bg-[#30d158]/12")} />
                    <RestPicker value={restOf(set)} onChange={(restSeconds) => updateSet(i, { restSeconds })} label={`Ruhezeit nach Satz ${i + 1}`}
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

/** Cardio like running or rowing: only its minutes, checked off like a set. */
function CardioLog({ item, previous, onChange, onRemove }: {
    item: SessionExercise, previous: number | null, onChange: (item: SessionExercise) => void, onRemove: () => void
}) {
    const cardio = item.cardio!
    const update = (next: Partial<typeof cardio>) => onChange({ ...item, cardio: { ...cardio, ...next } })
    return <Card className={cn("transition-shadow", cardio.done && "ring-2 ring-[#30d158]/40")}>
        <div className="flex items-center gap-3">
            <span className="grid size-10 flex-none place-items-center rounded-[10px] bg-[#ff375f]/12 text-[#ff375f]"><HeartPulse className="size-5" /></span>
            <div className="min-w-0 flex-1">
                <h2 className="truncate text-[17px] font-semibold">{item.name}</h2>
                <p className="truncate text-sm text-muted-foreground">{previous != null ? `Letztes Mal: ${previous} min` : "Cardio"}</p>
            </div>
            <div className="flex w-28 items-center gap-1.5">
                <NumberInput value={cardio.minutes} onChange={(minutes) => update({ minutes: Math.max(0, Math.round(minutes)) })}
                    label={`Minuten ${item.name}`} className={cn(cardio.done && "bg-[#30d158]/12")} />
                <span className="text-sm text-muted-foreground">min</span>
            </div>
            <button type="button" aria-label={cardio.done ? `${item.name} nicht erledigt` : `${item.name} erledigt`} aria-pressed={cardio.done}
                onClick={() => update({ done: !cardio.done })}
                className={cn("grid h-9 w-10 place-items-center rounded-[10px] transition-colors",
                    cardio.done ? "bg-[#30d158] text-white" : "bg-muted text-muted-foreground hover:text-foreground")}>
                <Check className="size-5" strokeWidth={3} />
            </button>
            <button type="button" aria-label="Cardio entfernen" title="Cardio entfernen" onClick={onRemove}
                className="grid size-8 flex-none place-items-center rounded-full text-[#ff453a] hover:bg-muted"><Trash2 className="size-4" /></button>
        </div>
    </Card>
}

/** A short note for the exercise, it is taken into the next workout with it. */
function NoteField({ note, onChange }: { note: string | null, onChange: (note: string | null) => void }) {
    const [open, setOpen] = useState(false)
    if (note == null && !open) {
        return <button type="button" onClick={() => setOpen(true)}
            className="mb-3 flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground">
            <StickyNote className="size-4" />Notiz hinzufügen
        </button>
    }
    return <div className="mb-3 flex items-start gap-2 rounded-[12px] bg-[#ffd60a]/15 px-3 py-2">
        <StickyNote className="mt-0.5 size-4 flex-none text-[#b38f00]" />
        <textarea value={note ?? ""} autoFocus={note == null} rows={1} maxLength={500} placeholder="z. B. Sitz auf Stufe 4"
            aria-label="Notiz" onChange={(e) => onChange(e.target.value || null)}
            onBlur={() => { if (!note?.trim()) { onChange(null); setOpen(false) } }}
            className="field-sizing-content min-h-5 w-full resize-none bg-transparent text-sm outline-none placeholder:text-muted-foreground" />
    </div>
}

// MARK: bottom bar

function BottomBar({ elapsed, now, rest, setRest, progress }: {
    elapsed: number, now: number, rest: Rest | null, setRest: (rest: Rest | null) => void, progress: string
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
    const cardio = items.flatMap((e) => e.cardio ? [e.cardio] : [])
    const cardioDone = cardio.filter((c) => c.done && c.minutes > 0)
    const open_ = sets.length - done.length + cardio.length - cardioDone.length
    // done cardio alone is worth saving too
    const anyDone = done.length + cardioDone.length

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
                <DialogTitle className="text-xl">{anyDone ? "Training beenden?" : "Noch kein Satz erledigt"}</DialogTitle>
                <DialogDescription>
                    {anyDone
                        ? open_ > 0 ? `${open_} ${open_ === 1 ? "Eintrag ist" : "Einträge sind"} nicht abgehakt und ${open_ === 1 ? "wird" : "werden"} nicht gespeichert.` : "Starke Leistung, alles erledigt!"
                        : "Hake erledigte Sätze ab, damit sie gespeichert werden – oder verwirf das Training."}
                </DialogDescription>
            </DialogHeader>
            <dl className="grid grid-cols-3 gap-3 rounded-[14px] bg-muted/60 p-4">
                <Stat label="Zeit" value={formatDuration(elapsed)} />
                <Stat label="Sätze" value={`${done.length}`} />
                {cardioDone.length && !done.length
                    ? <Stat label="Cardio" value={`${cardioDone.reduce((sum, c) => sum + c.minutes, 0)}`} unit="min" />
                    : <Stat label="Volumen" value={vol} unit={volUnit} />}
            </dl>
            {error && <p className="text-sm font-medium text-[#ff453a]">Beenden fehlgeschlagen, versuche es noch einmal.</p>}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button onClick={() => onOpenChange(false)} disabled={pending} className="rounded-full bg-muted px-4 py-2.5 font-semibold disabled:opacity-50">Weiter trainieren</button>
                {anyDone
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

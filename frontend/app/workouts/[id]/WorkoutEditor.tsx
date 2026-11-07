'use client'

import { AvatarImage } from "@/components/AvatarImage"
import { Card, CardHead, IconTile, Stat } from "@/components/dashboard"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { CardioExercise, ExerciseSet, Workout, WorkoutExercise, WorkoutExerciseType } from "@/utils/types/types"
import { Friend } from "@/utils/types/friend"
import { Exercise } from "@/utils/types/workoutTypes"
import { ArrowDown, ArrowUp, Check, ChevronLeft, HeartPulse, Play, Plus, Trash2, Users, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useMemo, useState, useTransition } from "react"
import { deleteWorkout, leaveWorkout, saveWorkout, shareWorkout, startSession } from "../action"
import ExercisePicker from "../ExercisePicker"
import NumberInput from "../NumberInput"
import RestPicker from "../RestPicker"
import MuscleMap from "../MuscleMap"
import { DEFAULT_CARDIO_MINUTES, DEFAULT_REST, formatRest, formatVolume, formatWeight, isCardio, MUSCLES, restOf, setsPerMuscle, volume } from "../stats"

const DEFAULT_SET: ExerciseSet = { order: 0, reps: 10, weightKg: 20, rpe: null, restSeconds: DEFAULT_REST }

export default function WorkoutEditor({ workout, exercises, friends }: { workout: Workout | null, exercises: Exercise[], friends: Friend[] }) {
    const router = useRouter()
    const [name, setName] = useState(workout?.name ?? "")
    const [items, setItems] = useState<WorkoutExercise[]>(workout?.workoutExercises ?? [])
    const [dirty, setDirty] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [pending, startPending] = useTransition()
    // templates of others can be looked at and trained, only their creator can change them
    const readOnly = !!workout && !workout.editable

    const exerciseById = useMemo(() => new Map(exercises.map((e) => [e.id, e])), [exercises])
    const strengthExercises = useMemo(() => exercises.filter((e) => !isCardio(e)), [exercises])
    const cardioExercises = useMemo(() => exercises.filter(isCardio), [exercises])
    const cardioMinutes = items.reduce((sum, e) => sum + (e.type === WorkoutExerciseType.CARDIO_EXERCISE ? e.minutes : 0), 0)
    const muscles = useMemo(() => setsPerMuscle(items.map((e) => ({ exerciseId: e.exercise.id, sets: e.sets.length })), exerciseById), [items, exerciseById])
    const sets = items.flatMap((e) => e.sets)
    const [vol, volUnit] = formatVolume(volume(sets))

    const change = (next: WorkoutExercise[]) => {
        setItems(next)
        setDirty(true)
    }
    const update = (index: number, item: WorkoutExercise) => change(items.map((e, i) => i === index ? item : e))
    const move = (index: number, by: number) => {
        const next = [...items]
        const [item] = next.splice(index, 1)
        next.splice(index + by, 0, item)
        change(next)
    }
    const add = (exercise: Exercise) => {
        // functional update, several exercises can be picked before the next render
        setItems((prev) => [...prev, { type: WorkoutExerciseType.STRENGTH_EXERCISE, exercise, sets: [{ ...DEFAULT_SET }, { ...DEFAULT_SET }, { ...DEFAULT_SET }] }])
        setDirty(true)
    }
    const addCardio = (exercise: Exercise) => {
        setItems((prev) => [...prev, { type: WorkoutExerciseType.CARDIO_EXERCISE, exercise, minutes: DEFAULT_CARDIO_MINUTES, sets: [] }])
        setDirty(true)
    }

    const save = async () => {
        const saved = await saveWorkout({ id: workout?.id ?? null, name: name.trim() || "Neues Workout", workoutExercises: items })
        setDirty(false)
        return saved
    }

    const run = (action: () => Promise<void>) => startPending(async () => {
        setError(null)
        try {
            await action()
        } catch {
            setError("Speichern fehlgeschlagen, versuche es noch einmal.")
        }
    })

    const onSave = () => run(async () => {
        const saved = await save()
        if (!workout) router.replace(`/workouts/${saved.id}`)
        else router.refresh()
    })
    const onStart = () => run(async () => {
        const saved = !readOnly && (dirty || !workout) ? await save() : workout!
        await startSession(saved.id)
        router.push("/workouts/session")
    })
    const onLeave = () => {
        if (!workout || !confirm(`„${workout.name}“ aus deiner Liste entfernen? ${workout.ownerName ?? "Dein Freund"} behält das Workout.`)) return
        run(async () => {
            await leaveWorkout(workout.id)
            router.push("/workouts")
        })
    }
    const onDelete = () => {
        if (!workout || !confirm(`Vorlage „${workout.name}“ löschen? Deine bisherigen Trainings bleiben erhalten.`)) return
        run(async () => {
            await deleteWorkout(workout.id)
            router.push("/workouts")
        })
    }

    const actions = <>
        {workout?.sharedWithMe && <button onClick={onLeave} disabled={pending} className={buttonVariants({ variant: "destructive" })}>Entfernen</button>}
        {workout && !readOnly && <button onClick={onDelete} disabled={pending} className={buttonVariants({ variant: "destructive" })}>Löschen</button>}
        {!readOnly && <button onClick={onSave} disabled={pending || (!!workout && !dirty)} className={cn(buttonVariants({ variant: "outline" }), "flex-1 md:flex-none")}>
            {pending ? "Speichert…" : "Speichern"}
        </button>}
        <button onClick={onStart} disabled={pending || !items.length} className={cn(buttonVariants(), "flex-1 md:flex-none")}>
            <Play className="size-3.5 fill-current" />Training starten
        </button>
    </>

    return <div className="w-full flex-1 px-3.5 pt-5 pb-28 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 md:gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-0 flex-1">
                    <Link href="/workouts" className="flex w-fit items-center text-label font-semibold text-brand-strong hover:underline"><ChevronLeft className="size-4" />Training</Link>
                    {readOnly
                        ? <h1 className="mt-1 truncate text-[22px] font-bold leading-7 tracking-tight">{name}</h1>
                        : <input value={name} onChange={(e) => { setName(e.target.value); setDirty(true) }} placeholder="Name der Vorlage" aria-label="Name"
                            className="mt-1 w-full rounded-[8px] bg-transparent text-[22px] font-bold leading-7 tracking-tight outline-none focus-visible:outline-2 focus-visible:outline-brand placeholder:text-ink-muted/50" />}
                    <p className="text-ink-muted">
                        {readOnly
                            ? `${workout.sharedWithMe ? "Geteilt von" : "Vorlage von"} ${workout.ownerName ?? "jemand anderem"} · nur ansehen`
                            : workout ? (dirty ? "Ungespeicherte Änderungen" : "Gespeichert") : "Neue Vorlage"}
                    </p>
                </div>
                <div className="hidden flex-wrap items-center gap-2 md:flex">{actions}</div>
            </div>
            {error && <p className="rounded-[12px] bg-danger-soft px-4 py-2 text-body font-medium text-danger">{error}</p>}

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <div className="flex flex-col gap-3 md:gap-4 lg:col-span-8">
                    {items.map((item, index) => {
                        const props = {
                            readOnly,
                            onRemove: () => change(items.filter((_, i) => i !== index)),
                            onMoveUp: index > 0 ? () => move(index, -1) : undefined,
                            onMoveDown: index < items.length - 1 ? () => move(index, 1) : undefined,
                        }
                        return item.type === WorkoutExerciseType.CARDIO_EXERCISE
                            ? <CardioCard key={index} item={item} onChange={(next) => update(index, next)} {...props} />
                            : <ExerciseCard key={index} item={item} exercise={exerciseById.get(item.exercise.id)} onChange={(next) => update(index, next)} {...props} />
                    })}
                    {!readOnly && <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <ExercisePicker exercises={strengthExercises} onPick={add}
                            className="flex items-center justify-center gap-2 rounded-[14px] border border-dashed border-ink-muted/40 p-5 font-semibold text-brand-strong transition-colors hover:bg-surface-raised/50 dark:hover:bg-white/5">
                            <Plus className="size-5" />Übung hinzufügen
                        </ExercisePicker>
                        <ExercisePicker exercises={cardioExercises} onPick={addCardio} title="Cardio hinzufügen"
                            className="flex items-center justify-center gap-2 rounded-[14px] border border-dashed border-ink-muted/40 p-5 font-semibold text-brand-strong transition-colors hover:bg-surface-raised/50 dark:hover:bg-white/5">
                            <HeartPulse className="size-5" />Cardio hinzufügen
                        </ExercisePicker>
                    </div>}
                    {readOnly && !items.length && <p className="py-6 text-center text-ink-muted">Keine Übungen</p>}
                </div>

                <div className="flex flex-col gap-4 lg:col-span-4">
                    <Card className="p-4 md:p-5">
                        <CardHead title="Übersicht" />
                        <dl className="grid grid-cols-3 gap-3">
                            <Stat label="Übungen" value={`${items.length}`} />
                            <Stat label="Sätze" value={`${sets.length}`} />
                            {cardioMinutes > 0
                                ? <Stat label="Cardio" value={`${cardioMinutes}`} unit="min" />
                                : <Stat label="Volumen" value={vol} unit={volUnit} />}
                        </dl>
                    </Card>
                    {workout && !readOnly && <ShareCard workout={workout} friends={friends} />}
                    <Card className="p-4 md:p-5">
                        <CardHead title="Muskeln" right="Sätze pro Muskel" />
                        <MuscleMap muscles={muscles} />
                    </Card>
                </div>
            </div>
        </div>

        {/* on phones the actions stay reachable at the bottom */}
        <div className="fixed inset-x-0 bottom-0 z-30 px-3 pb-[max(12px,env(safe-area-inset-bottom))] md:hidden">
            <div className="glass-thick mx-auto flex max-w-3xl items-center gap-2 rounded-[18px] p-2">
                {actions}
            </div>
        </div>
    </div>
}

/** Friends the template is shared with, they see and train it but can't change it. */
function ShareCard({ workout, friends }: { workout: Workout, friends: Friend[] }) {
    const router = useRouter()
    const [shared, setShared] = useState(() => new Set(workout.sharedWith.map((u) => u.id)))
    const [pending, start] = useTransition()
    const [error, setError] = useState(false)

    const toggle = (friendId: string) => {
        const next = new Set(shared)
        if (next.has(friendId)) next.delete(friendId)
        else next.add(friendId)
        const before = shared
        setShared(next)
        setError(false)
        start(async () => {
            try {
                await shareWorkout(workout.id, [...next])
                router.refresh()
            } catch {
                setShared(before)
                setError(true)
            }
        })
    }

    return <Card className="p-4 md:p-5">
        <CardHead title="Teilen" right={shared.size ? `mit ${shared.size} ${shared.size === 1 ? "Freund" : "Freunden"}` : "Nur für dich"} />
        {friends.length
            ? <div className="-mx-2 flex flex-col">
                {friends.map((friend) => {
                    const on = shared.has(friend.id)
                    return <button key={friend.id} type="button" onClick={() => toggle(friend.id)} disabled={pending} aria-pressed={on}
                        className="flex items-center gap-3 rounded-[12px] px-2 py-2 text-left hover:bg-surface-raised/50 dark:hover:bg-white/5 disabled:opacity-60">
                        <span className="grid size-8 flex-none place-items-center overflow-hidden rounded-full bg-track text-body font-semibold">
                            <AvatarImage src={friend.image} alt="" size={32} className="size-full object-cover" fallback={(friend.name?.trim() || "?").charAt(0).toUpperCase()} />
                        </span>
                        <span className="min-w-0 flex-1 truncate text-[14px] font-medium">{friend.name ?? "Unbekannt"}</span>
                        <span className={cn("grid size-6 place-items-center rounded-full", on ? "bg-brand text-on-brand" : "glass-inset text-transparent")}>
                            <Check className="size-3.5" strokeWidth={3} />
                        </span>
                    </button>
                })}
            </div>
            : <p className="flex items-center gap-2 text-body text-ink-muted">
                <Users className="size-4" />Füge unter <Link href="/friends" className="font-medium text-brand-strong">Freunde</Link> jemanden hinzu, um Workouts zu teilen.
            </p>}
        <p className="mt-2 text-label text-ink-muted">Freunde sehen und trainieren das Workout, ändern kannst nur du es.</p>
        {error && <p className="mt-2 text-body font-medium text-danger">Teilen fehlgeschlagen, versuche es noch einmal.</p>}
    </Card>
}

/** Cardio like running or rowing, only with its minutes. */
function CardioCard({ item, readOnly, onChange, onRemove, onMoveUp, onMoveDown }: {
    item: CardioExercise, readOnly: boolean, onChange: (item: CardioExercise) => void,
    onRemove: () => void, onMoveUp?: () => void, onMoveDown?: () => void
}) {
    return <Card className="p-4 md:p-5">
        <div className="flex items-center gap-1 md:gap-2">
            <IconTile icon={HeartPulse} size="sm" className="mr-2" />
            <div className="min-w-0 flex-1">
                <h2 className="truncate text-heading">{item.exercise.name}</h2>
                <p className="truncate text-caption text-ink-muted">Cardio</p>
            </div>
            {readOnly
                ? <span className="rounded-[10px] glass-inset px-3 py-2 text-[14px] font-semibold tabular-nums">{item.minutes} min</span>
                : <>
                    <div className="flex w-28 items-center gap-1.5">
                        <NumberInput value={item.minutes} onChange={(minutes) => onChange({ ...item, minutes: Math.max(0, Math.round(minutes)) })}
                            label={`Minuten ${item.exercise.name}`} className="h-10 md:h-9" />
                        <span className="text-body text-ink-muted">min</span>
                    </div>
                    <IconButton label="Nach oben" onClick={onMoveUp}><ArrowUp /></IconButton>
                    <IconButton label="Nach unten" onClick={onMoveDown}><ArrowDown /></IconButton>
                    <IconButton label="Cardio entfernen" onClick={onRemove} className="text-danger"><Trash2 /></IconButton>
                </>}
        </div>
    </Card>
}

function ExerciseCard({ item, exercise, readOnly, onChange, onRemove, onMoveUp, onMoveDown }: {
    item: WorkoutExercise, exercise?: Exercise, readOnly: boolean, onChange: (item: WorkoutExercise) => void,
    onRemove: () => void, onMoveUp?: () => void, onMoveDown?: () => void
}) {
    const muscles = [...new Set((exercise ?? item.exercise).bodyParts.map((p) => p.slug).filter(Boolean))]
    const updateSet = (index: number, set: Partial<ExerciseSet>) =>
        onChange({ ...item, sets: item.sets.map((s, i) => i === index ? { ...s, ...set } : s) })
    const addSet = () => onChange({ ...item, sets: [...item.sets, { ...(item.sets[item.sets.length - 1] ?? DEFAULT_SET) }] })

    return <Card className="p-4 md:p-5">
        <div className="mb-3 flex items-start gap-1 md:gap-2">
            <div className="min-w-0 flex-1">
                <h2 className="truncate text-heading">{item.exercise.name}</h2>
                <p className="truncate text-caption text-ink-muted">{muscles.map((m) => MUSCLES[m!] ?? m).join(", ") || item.exercise.description}</p>
            </div>
            {!readOnly && <>
                <IconButton label="Nach oben" onClick={onMoveUp}><ArrowUp /></IconButton>
                <IconButton label="Nach unten" onClick={onMoveDown}><ArrowDown /></IconButton>
                <IconButton label="Übung entfernen" onClick={onRemove} className="text-danger"><Trash2 /></IconButton>
            </>}
        </div>
        <div className={cn("grid items-center gap-x-2 gap-y-1.5 md:gap-x-3",
            readOnly
                ? "grid-cols-[24px_repeat(3,minmax(0,1fr))] md:grid-cols-[32px_repeat(3,minmax(0,1fr))]"
                : "grid-cols-[20px_repeat(3,minmax(0,1fr))_28px] md:grid-cols-[32px_repeat(3,minmax(0,1fr))_32px]")}>
            <span className="text-caption text-ink-muted">Satz</span>
            <span className="text-caption text-ink-muted">Wdh</span>
            <span className="text-caption text-ink-muted">kg</span>
            <span className="text-caption text-ink-muted">Pause</span>
            {!readOnly && <span />}
            {item.sets.map((set, i) => readOnly
                ? <ReadOnlySetRow key={i} index={i} set={set} />
                : <SetRow key={i} index={i} set={set} onChange={(s) => updateSet(i, s)}
                    onRemove={() => onChange({ ...item, sets: item.sets.filter((_, j) => j !== i) })} />)}
        </div>
        {!readOnly && <button onClick={addSet} className="mt-3 flex items-center gap-1 py-1 text-label font-semibold text-brand-strong hover:underline"><Plus className="size-4" />Satz hinzufügen</button>}
    </Card>
}

function SetRow({ index, set, onChange, onRemove }: { index: number, set: ExerciseSet, onChange: (set: Partial<ExerciseSet>) => void, onRemove: () => void }) {
    return <>
        <span className="text-center font-semibold tabular-nums text-ink-muted">{index + 1}</span>
        <NumberInput value={set.reps} onChange={(reps) => onChange({ reps: Math.round(reps) })} label={`Wiederholungen Satz ${index + 1}`} className="h-10 md:h-9" />
        <NumberInput value={set.weightKg} onChange={(weightKg) => onChange({ weightKg })} label={`Gewicht Satz ${index + 1}`} decimal className="h-10 md:h-9" />
        <RestPicker value={restOf(set)} onChange={(restSeconds) => onChange({ restSeconds })} label={`Ruhezeit nach Satz ${index + 1}`} className="h-10 md:h-9" />
        <IconButton label={`Satz ${index + 1} entfernen`} onClick={onRemove} className="size-7 md:size-8"><X /></IconButton>
    </>
}

function ReadOnlySetRow({ index, set }: { index: number, set: ExerciseSet }) {
    return <>
        <span className="text-center font-semibold tabular-nums text-ink-muted">{index + 1}</span>
        <span className="rounded-[10px] glass-inset px-3 py-2 text-[14px] font-semibold tabular-nums">{set.reps}</span>
        <span className="rounded-[10px] glass-inset px-3 py-2 text-[14px] font-semibold tabular-nums">{formatWeight(set.weightKg)}</span>
        <span className="rounded-[10px] glass-inset px-3 py-2 text-[14px] font-semibold tabular-nums">{formatRest(restOf(set))}</span>
    </>
}

function IconButton({ label, onClick, className, children }: { label: string, onClick?: () => void, className?: string, children: React.ReactNode }) {
    return <button type="button" aria-label={label} title={label} onClick={onClick} disabled={!onClick}
        className={cn("grid size-8 flex-none place-items-center rounded-full text-ink-muted hover:bg-surface-raised/50 dark:hover:bg-white/5 disabled:opacity-30 [&_svg]:size-4", className)}>
        {children}
    </button>
}

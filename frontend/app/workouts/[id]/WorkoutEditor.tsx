'use client'

import { Card, CardHead, Stat } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { ExerciseSet, Workout, WorkoutExercise, WorkoutExerciseType } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { ArrowDown, ArrowUp, ChevronLeft, Play, Plus, Trash2, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useMemo, useState, useTransition } from "react"
import { deleteWorkout, saveWorkout, startSession } from "../action"
import ExercisePicker from "../ExercisePicker"
import NumberInput from "../NumberInput"
import RestPicker from "../RestPicker"
import MuscleMap from "../MuscleMap"
import { DEFAULT_REST, formatRest, formatVolume, formatWeight, MUSCLES, restOf, setsPerMuscle, volume } from "../stats"

const DEFAULT_SET: ExerciseSet = { order: 0, reps: 10, weightKg: 20, rpe: null, restSeconds: DEFAULT_REST }

export default function WorkoutEditor({ workout, exercises }: { workout: Workout | null, exercises: Exercise[] }) {
    const router = useRouter()
    const [name, setName] = useState(workout?.name ?? "")
    const [items, setItems] = useState<WorkoutExercise[]>(workout?.workoutExercises ?? [])
    const [dirty, setDirty] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [pending, startPending] = useTransition()
    // templates of others can be looked at and trained, only their creator can change them
    const readOnly = !!workout && !workout.editable

    const exerciseById = useMemo(() => new Map(exercises.map((e) => [e.id, e])), [exercises])
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
    const onDelete = () => {
        if (!workout || !confirm(`Vorlage „${workout.name}“ löschen? Deine bisherigen Trainings bleiben erhalten.`)) return
        run(async () => {
            await deleteWorkout(workout.id)
            router.push("/workouts")
        })
    }

    const actions = <>
        {workout && !readOnly && <button onClick={onDelete} disabled={pending} className="rounded-full px-3.5 py-2 text-sm font-semibold text-[#ff453a] hover:bg-[#ff453a]/10 disabled:opacity-50">Löschen</button>}
        {!readOnly && <button onClick={onSave} disabled={pending || (!!workout && !dirty)} className="flex-1 rounded-full bg-background px-4 py-2.5 text-sm font-semibold shadow-[0_1px_3px_rgba(0,0,0,.12)] disabled:opacity-50 md:flex-none md:py-2">
            {pending ? "Speichert…" : "Speichern"}
        </button>}
        <button onClick={onStart} disabled={pending || !items.length} className="flex flex-1 items-center justify-center gap-1.5 rounded-full bg-[#30d158] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50 md:flex-none md:py-2">
            <Play className="size-3.5 fill-current" />Training starten
        </button>
    </>

    return <div className="flex-1 w-full bg-muted/50 px-3.5 pt-5 pb-28 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 md:gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-0 flex-1">
                    <Link href="/workouts" className="flex w-fit items-center text-[15px] text-[#0a84ff]"><ChevronLeft className="size-5" />Training</Link>
                    {readOnly
                        ? <h1 className="mt-1 truncate text-[28px] font-bold leading-tight tracking-tight md:text-[34px]">{name}</h1>
                        : <input value={name} onChange={(e) => { setName(e.target.value); setDirty(true) }} placeholder="Name der Vorlage" aria-label="Name"
                            className="mt-1 w-full bg-transparent text-[28px] font-bold leading-tight tracking-tight outline-none placeholder:text-muted-foreground/50 md:text-[34px]" />}
                    <p className="text-muted-foreground">
                        {readOnly
                            ? `Vorlage von ${workout.ownerName ?? "jemand anderem"} · nur ansehen`
                            : workout ? (dirty ? "Ungespeicherte Änderungen" : "Gespeichert") : "Neue Vorlage"}
                    </p>
                </div>
                <div className="hidden flex-wrap items-center gap-2 md:flex">{actions}</div>
            </div>
            {error && <p className="rounded-xl bg-[#ff453a]/10 px-4 py-2 text-sm font-medium text-[#ff453a]">{error}</p>}

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <div className="flex flex-col gap-3 md:gap-4 lg:col-span-8">
                    {items.map((item, index) => <ExerciseCard key={index} item={item} exercise={exerciseById.get(item.exercise.id)} readOnly={readOnly}
                        onChange={(next) => update(index, next)}
                        onRemove={() => change(items.filter((_, i) => i !== index))}
                        onMoveUp={index > 0 ? () => move(index, -1) : undefined}
                        onMoveDown={index < items.length - 1 ? () => move(index, 1) : undefined} />)}
                    {!readOnly && <ExercisePicker exercises={exercises} onPick={add}
                        className="flex items-center justify-center gap-2 rounded-[18px] border-2 border-dashed p-5 font-semibold text-[#0a84ff] hover:bg-card">
                        <Plus className="size-5" />Übung hinzufügen
                    </ExercisePicker>}
                    {readOnly && !items.length && <p className="py-6 text-center text-muted-foreground">Keine Übungen</p>}
                </div>

                <div className="flex flex-col gap-4 lg:col-span-4">
                    <Card className="p-4 md:p-5">
                        <CardHead title="Übersicht" />
                        <dl className="grid grid-cols-3 gap-3">
                            <Stat label="Übungen" value={`${items.length}`} />
                            <Stat label="Sätze" value={`${sets.length}`} />
                            <Stat label="Volumen" value={vol} unit={volUnit} />
                        </dl>
                    </Card>
                    <Card className="p-4 md:p-5">
                        <CardHead title="Muskeln" right="Sätze pro Muskel" />
                        <MuscleMap muscles={muscles} />
                    </Card>
                </div>
            </div>
        </div>

        {/* on phones the actions stay reachable at the bottom */}
        <div className="fixed inset-x-0 bottom-0 z-30 px-3 pb-[max(12px,env(safe-area-inset-bottom))] md:hidden">
            <div className="mx-auto flex max-w-3xl items-center gap-2 rounded-[22px] bg-background/85 p-2 shadow-[0_8px_32px_rgba(0,0,0,.18)] ring-1 ring-foreground/5 backdrop-blur-xl">
                {actions}
            </div>
        </div>
    </div>
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
                <h2 className="truncate text-[17px] font-semibold">{item.exercise.name}</h2>
                <p className="truncate text-sm text-muted-foreground">{muscles.map((m) => MUSCLES[m!] ?? m).join(", ") || item.exercise.description}</p>
            </div>
            {!readOnly && <>
                <IconButton label="Nach oben" onClick={onMoveUp}><ArrowUp /></IconButton>
                <IconButton label="Nach unten" onClick={onMoveDown}><ArrowDown /></IconButton>
                <IconButton label="Übung entfernen" onClick={onRemove} className="text-[#ff453a]"><Trash2 /></IconButton>
            </>}
        </div>
        <div className={cn("grid items-center gap-x-2 gap-y-1.5 md:gap-x-3",
            readOnly
                ? "grid-cols-[24px_repeat(3,minmax(0,1fr))] md:grid-cols-[32px_repeat(3,minmax(0,1fr))]"
                : "grid-cols-[20px_repeat(3,minmax(0,1fr))_28px] md:grid-cols-[32px_repeat(3,minmax(0,1fr))_32px]")}>
            <span className="text-xs font-medium text-muted-foreground">Satz</span>
            <span className="text-xs font-medium text-muted-foreground">Wdh</span>
            <span className="text-xs font-medium text-muted-foreground">kg</span>
            <span className="text-xs font-medium text-muted-foreground">Pause</span>
            {!readOnly && <span />}
            {item.sets.map((set, i) => readOnly
                ? <ReadOnlySetRow key={i} index={i} set={set} />
                : <SetRow key={i} index={i} set={set} onChange={(s) => updateSet(i, s)}
                    onRemove={() => onChange({ ...item, sets: item.sets.filter((_, j) => j !== i) })} />)}
        </div>
        {!readOnly && <button onClick={addSet} className="mt-3 flex items-center gap-1 py-1 text-sm font-medium text-[#0a84ff]"><Plus className="size-4" />Satz hinzufügen</button>}
    </Card>
}

function SetRow({ index, set, onChange, onRemove }: { index: number, set: ExerciseSet, onChange: (set: Partial<ExerciseSet>) => void, onRemove: () => void }) {
    return <>
        <span className="text-center font-semibold tabular-nums text-muted-foreground">{index + 1}</span>
        <NumberInput value={set.reps} onChange={(reps) => onChange({ reps: Math.round(reps) })} label={`Wiederholungen Satz ${index + 1}`} className="h-10 md:h-9" />
        <NumberInput value={set.weightKg} onChange={(weightKg) => onChange({ weightKg })} label={`Gewicht Satz ${index + 1}`} decimal className="h-10 md:h-9" />
        <RestPicker value={restOf(set)} onChange={(restSeconds) => onChange({ restSeconds })} label={`Ruhezeit nach Satz ${index + 1}`} className="h-10 md:h-9" />
        <IconButton label={`Satz ${index + 1} entfernen`} onClick={onRemove} className="size-7 md:size-8"><X /></IconButton>
    </>
}

function ReadOnlySetRow({ index, set }: { index: number, set: ExerciseSet }) {
    return <>
        <span className="text-center font-semibold tabular-nums text-muted-foreground">{index + 1}</span>
        <span className="rounded-[10px] bg-muted/60 px-3 py-2 text-[15px] font-semibold tabular-nums">{set.reps}</span>
        <span className="rounded-[10px] bg-muted/60 px-3 py-2 text-[15px] font-semibold tabular-nums">{formatWeight(set.weightKg)}</span>
        <span className="rounded-[10px] bg-muted/60 px-3 py-2 text-[15px] font-semibold tabular-nums">{formatRest(restOf(set))}</span>
    </>
}

function IconButton({ label, onClick, className, children }: { label: string, onClick?: () => void, className?: string, children: React.ReactNode }) {
    return <button type="button" aria-label={label} title={label} onClick={onClick} disabled={!onClick}
        className={cn("grid size-8 flex-none place-items-center rounded-full text-muted-foreground hover:bg-muted disabled:opacity-30 [&_svg]:size-4", className)}>
        {children}
    </button>
}

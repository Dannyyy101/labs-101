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
import { DEFAULT_REST, formatVolume, MUSCLES, restOf, setsPerMuscle, volume } from "../stats"

const DEFAULT_SET: ExerciseSet = { order: 0, reps: 10, weightKg: 20, rpe: null, restSeconds: DEFAULT_REST }

export default function WorkoutEditor({ workout, exercises }: { workout: Workout | null, exercises: Exercise[] }) {
    const router = useRouter()
    const [name, setName] = useState(workout?.name ?? "")
    const [items, setItems] = useState<WorkoutExercise[]>(workout?.workoutExercises ?? [])
    const [dirty, setDirty] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [pending, startPending] = useTransition()

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
        const saved = dirty || !workout ? await save() : workout
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

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-64 flex-1">
                    <Link href="/workouts" className="flex w-fit items-center text-[15px] text-[#0a84ff]"><ChevronLeft className="size-5" />Training</Link>
                    <input value={name} onChange={(e) => { setName(e.target.value); setDirty(true) }} placeholder="Name der Vorlage" aria-label="Name"
                        className="mt-1 w-full bg-transparent text-[34px] font-bold leading-tight tracking-tight outline-none placeholder:text-muted-foreground/50" />
                    <p className="text-muted-foreground">{workout ? (dirty ? "Ungespeicherte Änderungen" : "Gespeichert") : "Neue Vorlage"}</p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                    {workout && <button onClick={onDelete} disabled={pending} className="rounded-full px-3.5 py-2 text-sm font-semibold text-[#ff453a] hover:bg-[#ff453a]/10 disabled:opacity-50">Löschen</button>}
                    <button onClick={onSave} disabled={pending || (!!workout && !dirty)} className="rounded-full bg-background px-4 py-2 text-sm font-semibold shadow-[0_1px_3px_rgba(0,0,0,.12)] disabled:opacity-50">
                        {pending ? "Speichert…" : "Speichern"}
                    </button>
                    <button onClick={onStart} disabled={pending || !items.length} className="flex items-center gap-1.5 rounded-full bg-[#30d158] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
                        <Play className="size-3.5 fill-current" />Training starten
                    </button>
                </div>
            </div>
            {error && <p className="rounded-xl bg-[#ff453a]/10 px-4 py-2 text-sm font-medium text-[#ff453a]">{error}</p>}

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <div className="flex flex-col gap-4 lg:col-span-8">
                    {items.map((item, index) => <ExerciseCard key={index} item={item} exercise={exerciseById.get(item.exercise.id)}
                        onChange={(next) => update(index, next)}
                        onRemove={() => change(items.filter((_, i) => i !== index))}
                        onMoveUp={index > 0 ? () => move(index, -1) : undefined}
                        onMoveDown={index < items.length - 1 ? () => move(index, 1) : undefined} />)}
                    <ExercisePicker exercises={exercises} onPick={add}
                        className="flex items-center justify-center gap-2 rounded-[18px] border-2 border-dashed p-5 font-semibold text-[#0a84ff] hover:bg-card">
                        <Plus className="size-5" />Übung hinzufügen
                    </ExercisePicker>
                </div>

                <div className="flex flex-col gap-4 lg:col-span-4">
                    <Card>
                        <CardHead title="Übersicht" />
                        <dl className="grid grid-cols-3 gap-3">
                            <Stat label="Übungen" value={`${items.length}`} />
                            <Stat label="Sätze" value={`${sets.length}`} />
                            <Stat label="Volumen" value={vol} unit={volUnit} />
                        </dl>
                    </Card>
                    <Card>
                        <CardHead title="Muskeln" right="Sätze pro Muskel" />
                        <MuscleMap muscles={muscles} />
                    </Card>
                </div>
            </div>
        </div>
    </div>
}

function ExerciseCard({ item, exercise, onChange, onRemove, onMoveUp, onMoveDown }: {
    item: WorkoutExercise, exercise?: Exercise, onChange: (item: WorkoutExercise) => void,
    onRemove: () => void, onMoveUp?: () => void, onMoveDown?: () => void
}) {
    const muscles = [...new Set((exercise ?? item.exercise).bodyParts.map((p) => p.slug).filter(Boolean))]
    const updateSet = (index: number, set: Partial<ExerciseSet>) =>
        onChange({ ...item, sets: item.sets.map((s, i) => i === index ? { ...s, ...set } : s) })
    const addSet = () => onChange({ ...item, sets: [...item.sets, { ...(item.sets[item.sets.length - 1] ?? DEFAULT_SET) }] })

    return <Card>
        <div className="mb-3 flex items-start gap-2">
            <div className="min-w-0 flex-1">
                <h2 className="truncate text-[17px] font-semibold">{item.exercise.name}</h2>
                <p className="truncate text-sm text-muted-foreground">{muscles.map((m) => MUSCLES[m!] ?? m).join(", ") || item.exercise.description}</p>
            </div>
            <IconButton label="Nach oben" onClick={onMoveUp}><ArrowUp /></IconButton>
            <IconButton label="Nach unten" onClick={onMoveDown}><ArrowDown /></IconButton>
            <IconButton label="Übung entfernen" onClick={onRemove} className="text-[#ff453a]"><Trash2 /></IconButton>
        </div>
        <div className="grid grid-cols-[32px_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_32px] items-center gap-x-3 gap-y-1.5">
            <span className="text-xs font-medium text-muted-foreground">Satz</span>
            <span className="text-xs font-medium text-muted-foreground">Wdh</span>
            <span className="text-xs font-medium text-muted-foreground">kg</span>
            <span className="text-xs font-medium text-muted-foreground">Ruhezeit</span>
            <span />
            {item.sets.map((set, i) => <SetRow key={i} index={i} set={set} onChange={(s) => updateSet(i, s)}
                onRemove={() => onChange({ ...item, sets: item.sets.filter((_, j) => j !== i) })} />)}
        </div>
        <button onClick={addSet} className="mt-3 flex items-center gap-1 text-sm font-medium text-[#0a84ff]"><Plus className="size-4" />Satz hinzufügen</button>
    </Card>
}

function SetRow({ index, set, onChange, onRemove }: { index: number, set: ExerciseSet, onChange: (set: Partial<ExerciseSet>) => void, onRemove: () => void }) {
    return <>
        <span className="text-center font-semibold tabular-nums text-muted-foreground">{index + 1}</span>
        <NumberInput value={set.reps} onChange={(reps) => onChange({ reps: Math.round(reps) })} label={`Wiederholungen Satz ${index + 1}`} />
        <NumberInput value={set.weightKg} onChange={(weightKg) => onChange({ weightKg })} label={`Gewicht Satz ${index + 1}`} decimal />
        <RestPicker value={restOf(set)} onChange={(restSeconds) => onChange({ restSeconds })} label={`Ruhezeit nach Satz ${index + 1}`} />
        <IconButton label={`Satz ${index + 1} entfernen`} onClick={onRemove}><X /></IconButton>
    </>
}

function IconButton({ label, onClick, className, children }: { label: string, onClick?: () => void, className?: string, children: React.ReactNode }) {
    return <button type="button" aria-label={label} title={label} onClick={onClick} disabled={!onClick}
        className={cn("grid size-8 flex-none place-items-center rounded-full text-muted-foreground hover:bg-muted disabled:opacity-30 [&_svg]:size-4", className)}>
        {children}
    </button>
}

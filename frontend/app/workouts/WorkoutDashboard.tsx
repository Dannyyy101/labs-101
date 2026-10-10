'use client'

import { Card, CardHead, Delta, KpiGrid, Note, Period, PERIODS, periodRange, isInPeriod, Segmented } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { Workout, WorkoutSession } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { ChevronRight, Dumbbell, Pencil, Play, Plus, Trash2, Trophy } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useMemo, useState, useTransition } from "react"
import { de, formatDate, formatDuration, formatHours } from "../runs/format"
import { deleteSession, startSession } from "./action"
import { VolumeChart } from "./Charts"
import MuscleMap from "./MuscleMap"
import { useNow } from "./useNow"
import { aggregate, COLORS, doneSets, formatSet, formatVolume, formatWeight, MUSCLES, oneRepMax, sessionDuration, setsPerMuscle, volume } from "./stats"

const PAGE_SIZE = 8

const formatTime = (date: Date) => date.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })

export default function WorkoutDashboard({ workouts, sessions, active, exercises }: {
    workouts: Workout[], sessions: WorkoutSession[], active: WorkoutSession | null, exercises: Exercise[]
}) {
    const [period, setPeriod] = useState<Period>("30")
    const [expanded, setExpanded] = useState<number | null>(null)
    const [shown, setShown] = useState(PAGE_SIZE)

    const current = useMemo(() => periodRange(period), [period])
    const inPeriod = useMemo(() => sessions.filter((s) => isInPeriod(s.startedAt, current)), [sessions, current])
    const exerciseById = useMemo(() => new Map(exercises.map((e) => [e.id, e])), [exercises])
    const muscles = useMemo(() => setsPerMuscle(
        inPeriod.flatMap((s) => s.exercises.map((e) => ({ exerciseId: e.exerciseId, sets: e.sets.filter((x) => x.done).length }))),
        exerciseById), [inPeriod, exerciseById])

    const select = (session: WorkoutSession) => {
        setExpanded(session.id)
        const index = inPeriod.findIndex((s) => s.id === session.id)
        if (index >= shown) setShown(index + 1)
        // the row is rendered after the state update
        setTimeout(() => document.getElementById(`session-${session.id}`)?.scrollIntoView({ behavior: "smooth", block: "center" }))
    }

    const changePeriod = (value: Period) => {
        setPeriod(value)
        setShown(PAGE_SIZE)
    }

    if (!workouts.length && !sessions.length && !active) {
        return <div className="flex-1 w-full bg-muted/50 px-4 py-6 md:px-8">
            <div className="mx-auto max-w-7xl">
                <h1 className="text-4xl font-bold tracking-tight">Training</h1>
                <div className="mt-6 flex flex-col items-center rounded-[18px] bg-card p-10 text-center shadow-sm">
                    <div className="grid size-12 place-items-center rounded-[14px] text-white" style={{ background: COLORS.count }}><Dumbbell /></div>
                    <p className="mt-4 text-lg font-semibold">Noch keine Trainings</p>
                    <p className="mt-1 max-w-md text-muted-foreground">Lege eine Vorlage mit deinen Übungen an oder starte direkt ein freies Training und füge die Übungen unterwegs hinzu.</p>
                    <div className="mt-5 flex flex-wrap justify-center gap-2">
                        <Link href="/workouts/new" className="rounded-full bg-[#0a84ff] px-4 py-2 font-semibold text-white">Vorlage erstellen</Link>
                        <StartButton workoutId={null} className="rounded-full bg-muted px-4 py-2 font-semibold">Freies Training</StartButton>
                    </div>
                </div>
            </div>
        </div>
    }

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-56 flex-1">
                    <h1 className="text-[34px] font-bold leading-tight tracking-tight">Training</h1>
                    <p className="whitespace-nowrap text-muted-foreground">
                        {period === "year"
                            ? `1. Jan – ${formatDate(current[1])} ${current[1].getFullYear()}`
                            : `${formatDate(current[0])} – ${formatDate(current[1])}`}
                    </p>
                </div>
                <Segmented value={period} options={PERIODS} onChange={changePeriod} />
            </div>

            {active && <ActiveBanner session={active} />}

            <Kpis sessions={sessions} inPeriod={inPeriod} period={period} current={current} />

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <Card className="lg:col-span-8">
                    <CardHead title="Vorlagen" right={<Link href="/workouts/new" className="font-medium text-[#0a84ff]">+ Neue Vorlage</Link>} />
                    <Templates workouts={workouts} sessions={sessions} exercises={exerciseById} blocked={!!active} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Muskeln" right="Sätze im Zeitraum" />
                    <MuscleMap muscles={muscles} />
                </Card>

                <Card className="lg:col-span-8">
                    <CardHead title="Volumen" right={period === "year" ? "Tonnen pro Woche" : "Tonnen pro Tag"} />
                    <VolumeChart sessions={inPeriod} period={period} from={current[0]} to={current[1]} onSelect={select} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Bestleistungen" right={`${new Date().getFullYear()}`} />
                    <Records sessions={sessions} onSelect={select} />
                </Card>

                <Card className="lg:col-span-12">
                    <CardHead title="Trainings" right={`${inPeriod.length} im Zeitraum`} />
                    <div className="flex flex-col">
                        {inPeriod.slice(0, shown).map((session) =>
                            <SessionRow key={session.id} session={session} expanded={session.id === expanded}
                                onClick={() => setExpanded(session.id === expanded ? null : session.id)} />)}
                        {inPeriod.length > shown && <button onClick={() => setShown(shown + PAGE_SIZE)} className="mt-2 self-start px-3 py-1.5 font-medium text-[#0a84ff]">
                            {Math.min(PAGE_SIZE, inPeriod.length - shown)} weitere anzeigen
                        </button>}
                        {!inPeriod.length && <p className="py-6 text-center text-muted-foreground">Keine Trainings in diesem Zeitraum</p>}
                    </div>
                </Card>
            </div>
        </div>
    </div>
}

/** Starts the workout and opens it, `workoutId` null starts one without exercises. */
function StartButton({ workoutId, className, disabled, children }: { workoutId: number | null, className?: string, disabled?: boolean, children: React.ReactNode }) {
    const router = useRouter()
    const [pending, start] = useTransition()
    return <button type="button" disabled={disabled || pending} onClick={() => start(async () => {
        await startSession(workoutId)
        router.push("/workouts/session")
    })}
        className={cn("disabled:opacity-50", className)}>
        {pending ? "Startet…" : children}
    </button>
}

// MARK: active workout

function ActiveBanner({ session }: { session: WorkoutSession }) {
    const now = useNow()
    const sets = session.exercises.flatMap((e) => e.sets)
    const done = sets.filter((s) => s.done).length
    return <Link href="/workouts/session" className="flex items-center gap-4 rounded-[18px] bg-[#30d158] p-4 text-white shadow-[0_8px_24px_rgba(48,209,88,.3)] md:px-5">
        <span className="relative flex size-3 flex-none">
            <span className="absolute inline-flex size-full animate-ping rounded-full bg-white opacity-75" />
            <span className="relative inline-flex size-3 rounded-full bg-white" />
        </span>
        <div className="min-w-0 flex-1">
            <div className="text-sm font-medium opacity-85">Training läuft</div>
            <div className="truncate text-lg font-bold">{session.name}</div>
        </div>
        <div className="text-right">
            <div className="text-2xl font-bold tabular-nums">{formatDuration(sessionDuration(session, now))}</div>
            <div className="text-sm opacity-85">{done} / {sets.length} Sätze</div>
        </div>
        <ChevronRight className="size-5 flex-none" />
    </Link>
}

// MARK: KPIs

function Kpis({ sessions, inPeriod, period, current }: { sessions: WorkoutSession[], inPeriod: WorkoutSession[], period: Period, current: [Date, Date] }) {
    const cur = aggregate(inPeriod)
    const prev = period === "year" ? null : aggregate(sessions.filter((s) => isInPeriod(s.startedAt, periodRange(period, 1))))
    const weeks = Math.max(1, (current[1].getTime() - current[0].getTime()) / 864e5 / 7)

    const time = formatHours(cur.time).split(" ")
    const [vol, volUnit] = formatVolume(cur.volume)
    return <KpiGrid items={[
        {
            label: "Trainings", color: COLORS.count,
            value: cur.count,
            delta: prev ? <Delta current={cur.count} previous={prev.count} /> : <Note>Ø {de(cur.count / weeks, 1)} / Woche</Note>,
        },
        {
            label: "Zeit", color: COLORS.time,
            value: <>{time.map((t, i) => i % 2 ? <small key={i}>{t} </small> : <span key={i}>{t} </span>)}</>,
            delta: prev ? <Delta current={cur.time} previous={prev.time} /> : <Note>{cur.count ? `Ø ${formatHours(cur.time / cur.count)} / Training` : " "}</Note>,
        },
        {
            label: "Volumen", color: COLORS.volume,
            value: <>{vol} <small>{volUnit}</small></>,
            delta: prev ? <Delta current={cur.volume} previous={prev.volume} /> : <Note />,
        },
        {
            label: "Sätze", color: COLORS.sets,
            value: cur.sets,
            delta: prev ? <Delta current={cur.sets} previous={prev.sets} /> : <Note>Ø {de(cur.sets / weeks)} / Woche</Note>,
        },
        {
            label: "Wiederholungen", color: COLORS.reps,
            value: de(cur.reps),
            delta: prev ? <Delta current={cur.reps} previous={prev.reps} /> : <Note />,
        },
    ]} />
}

// MARK: cards

function Templates({ workouts, sessions, exercises, blocked }: { workouts: Workout[], sessions: WorkoutSession[], exercises: Map<number, Exercise>, blocked: boolean }) {
    return <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {workouts.map((workout) => {
            const sets = workout.workoutExercises.reduce((sum, e) => sum + e.sets.length, 0)
            const last = sessions.find((s) => s.workoutId === workout.id)
            const muscles = setsPerMuscle(workout.workoutExercises.map((e) => ({ exerciseId: e.exercise.id, sets: e.sets.length })), exercises)
            return <div key={workout.id} className="flex flex-col rounded-[14px] bg-muted/60 p-4">
                <div className="flex items-start gap-2">
                    <div className="min-w-0 flex-1">
                        <div className="truncate text-[17px] font-semibold">{workout.name}</div>
                        <div className="text-sm text-muted-foreground">
                            {workout.workoutExercises.length} {workout.workoutExercises.length === 1 ? "Übung" : "Übungen"} · {sets} {sets === 1 ? "Satz" : "Sätze"}
                        </div>
                    </div>
                    <Link href={`/workouts/${workout.id}`} aria-label={`${workout.name} bearbeiten`} className="grid size-8 flex-none place-items-center rounded-full text-muted-foreground hover:bg-background">
                        <Pencil className="size-4" />
                    </Link>
                </div>
                <div className="mt-2 line-clamp-2 min-h-10 text-sm">
                    {workout.workoutExercises.map((e) => e.exercise.name).join(" · ") || <span className="text-muted-foreground">Keine Übungen</span>}
                </div>
                {muscles.length > 0 && <div className="mt-2 flex flex-wrap gap-1">
                    {muscles.slice(0, 3).map(([slug]) => <span key={slug} className="rounded-full bg-[#ff9f0a]/15 px-2 py-0.5 text-xs font-medium text-[#c76f00] dark:text-[#ff9f0a]">{MUSCLES[slug] ?? slug}</span>)}
                </div>}
                <div className="mt-auto flex items-center gap-2 pt-3">
                    <span className="min-w-0 flex-1 truncate text-xs text-muted-foreground">{last ? `Zuletzt ${formatDate(new Date(last.startedAt))}` : "Noch nie trainiert"}</span>
                    <StartButton workoutId={workout.id} disabled={blocked}
                        className="flex items-center gap-1.5 rounded-full bg-[#30d158] px-3.5 py-1.5 text-sm font-semibold text-white">
                        <Play className="size-3.5 fill-current" />Starten
                    </StartButton>
                </div>
            </div>
        })}
        <div className="flex min-h-40 flex-col items-center justify-center gap-2 rounded-[14px] border-2 border-dashed p-4 text-center">
            <StartButton workoutId={null} disabled={blocked} className="flex items-center gap-1.5 rounded-full bg-muted px-3.5 py-1.5 text-sm font-semibold">
                <Play className="size-3.5 fill-current" />Freies Training
            </StartButton>
            <Link href="/workouts/new" className="flex items-center gap-1 py-1 text-sm font-medium text-[#0a84ff]"><Plus className="size-4" />Vorlage erstellen</Link>
        </div>
    </div>
}

const RECORD_COLORS = ["#ff9f0a", "#0a84ff", "#30d158", "#ff375f", "#bf5af2", "#64d2ff"]

/** The heaviest set (by estimated one rep max) of the most trained exercises this year. */
function Records({ sessions, onSelect }: { sessions: WorkoutSession[], onSelect: (session: WorkoutSession) => void }) {
    const year = new Date().getFullYear()
    const best = new Map<string, { name: string, session: WorkoutSession, reps: number, weightKg: number, max: number, count: number }>()
    for (const session of sessions) {
        if (new Date(session.startedAt).getFullYear() !== year) continue
        for (const exercise of session.exercises) {
            const key = exercise.exerciseId != null ? `${exercise.exerciseId}` : exercise.name
            for (const set of exercise.sets) {
                if (!set.done || !set.weightKg) continue
                const current = best.get(key)
                const max = oneRepMax(set)
                if (!current || max > current.max) best.set(key, { name: exercise.name, session, reps: set.reps, weightKg: set.weightKg, max, count: (current?.count ?? 0) + 1 })
                else current.count++
            }
        }
    }
    const rows = [...best.values()].sort((a, b) => b.count - a.count).slice(0, 6)
    if (!rows.length) return <p className="py-6 text-sm text-muted-foreground">Noch keine Sätze mit Gewicht in {year}</p>

    return <div className="flex flex-col">
        {rows.map((row, i) => <button key={row.name + i} onClick={() => onSelect(row.session)}
            className={cn("flex items-center gap-3 py-2.5 text-left", i > 0 && "border-t")}>
            <div className="grid size-[34px] flex-none place-items-center rounded-[10px] text-white" style={{ background: RECORD_COLORS[i % RECORD_COLORS.length] }}>
                <Trophy className="size-[18px]" strokeWidth={2.2} />
            </div>
            <div className="min-w-0 flex-1">
                <div className="truncate font-semibold">{row.name}</div>
                <div className="text-xs text-muted-foreground">{formatDate(new Date(row.session.startedAt))} · {formatSet(row)}</div>
            </div>
            <div className="text-right">
                <div className="text-[17px] font-semibold tabular-nums">{formatWeight(Math.round(row.max))} kg</div>
                <div className="text-[11px] text-muted-foreground">1RM geschätzt</div>
            </div>
        </button>)}
    </div>
}

function SessionRow({ session, expanded, onClick }: { session: WorkoutSession, expanded: boolean, onClick: () => void }) {
    const [deleting, startDelete] = useTransition()
    const sets = doneSets(session)
    const [vol, volUnit] = formatVolume(volume(sets))
    const start = new Date(session.startedAt)

    const remove = () => {
        if (confirm(`„${session.name}“ vom ${formatDate(start)} löschen?`)) startDelete(() => deleteSession(session.id))
    }

    return <div id={`session-${session.id}`} className={cn("rounded-xl", expanded && "bg-[#0a84ff]/8")}>
        <button onClick={onClick} className={cn(
            "grid w-full grid-cols-[40px_minmax(0,1fr)_auto] items-center gap-4 rounded-xl px-3 py-2.5 text-left md:grid-cols-[40px_minmax(0,1.4fr)_repeat(4,minmax(0,1fr))_16px]",
            !expanded && "hover:bg-muted")}>
            <div className="grid size-10 place-items-center rounded-[10px] text-white" style={{ background: expanded ? "#0a84ff" : COLORS.count }}>
                <Dumbbell className="size-5" />
            </div>
            <div className="min-w-0">
                <div className="truncate text-[15px] font-semibold">{session.name}</div>
                <div className="truncate text-xs text-muted-foreground">{formatDate(start)} · {formatTime(start)} Uhr</div>
            </div>
            <Cell value={formatDuration(sessionDuration(session))} label="Zeit" />
            <Cell value={`${session.exercises.length}`} label="Übungen" hide />
            <Cell value={`${sets.length}`} label="Sätze" hide />
            <Cell value={vol} label={volUnit} hide />
            <ChevronRight className={cn("hidden size-3.5 text-muted-foreground/50 transition-transform md:block", expanded && "rotate-90")} strokeWidth={2.6} />
        </button>
        {expanded && <div className="px-3 pb-3 md:pl-[72px]">
            <div className="flex flex-col divide-y rounded-xl bg-card">
                {session.exercises.map((exercise, i) => <div key={i} className="flex flex-col gap-1.5 px-3 py-2.5 md:flex-row md:items-center md:gap-4">
                    <div className="font-semibold md:w-56 md:flex-none">{exercise.name}</div>
                    <div className="flex flex-wrap gap-1.5">
                        {exercise.sets.filter((s) => s.done).map((set, j) =>
                            <span key={j} className="rounded-lg bg-muted px-2 py-0.5 text-sm tabular-nums">{formatSet(set)}</span>)}
                    </div>
                </div>)}
                {!session.exercises.length && <p className="px-3 py-4 text-sm text-muted-foreground">Keine Sätze erfasst</p>}
            </div>
            <div className="mt-2 flex justify-end">
                <button onClick={remove} disabled={deleting} className="flex items-center gap-1 px-2 py-1 text-sm font-medium text-[#ff453a] disabled:opacity-50">
                    <Trash2 className="size-3.5" />{deleting ? "Löscht…" : "Training löschen"}
                </button>
            </div>
        </div>}
    </div>
}

function Cell({ value, label, hide }: { value: string, label: string, hide?: boolean }) {
    return <div className={cn("text-[15px] tabular-nums", hide && "hidden md:block")}>
        {value}<small className="block text-[11px] text-muted-foreground">{label}</small>
    </div>
}

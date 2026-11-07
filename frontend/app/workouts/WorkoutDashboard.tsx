'use client'

import { Card, CardHead, chip, Delta, IconTile, KpiGrid, neutralChip, Note, Period, PERIODS, periodRange, isInPeriod, Segmented } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { Workout, WorkoutExerciseType, WorkoutSession } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { buttonVariants } from "@/components/ui/button"
import { ChevronRight, Clock, Dumbbell, Eye, Layers, Pencil, Play, Plus, Repeat, Trash2, Trophy, Weight } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useMemo, useState, useTransition } from "react"
import { de, formatDate, formatDuration, formatHours } from "../runs/format"
import { deleteSession, startSession } from "./action"
import { VolumeChart } from "./Charts"
import MuscleMap from "./MuscleMap"
import { useNow } from "./useNow"
import { aggregate, doneSets, formatSet, formatVolume, formatWeight, MUSCLES, oneRepMax, sessionDuration, setsPerMuscle, volume } from "./stats"

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
        return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
            <div className="mx-auto max-w-7xl">
                <h1 className="text-[22px] font-bold leading-7 tracking-tight">Training</h1>
                <Card className="mt-5 flex flex-col items-center p-10 text-center">
                    <IconTile icon={Dumbbell} />
                    <p className="mt-4 text-heading">Noch keine Trainings</p>
                    <p className="mt-1 max-w-md text-ink-muted">Lege eine Vorlage mit deinen Übungen an oder starte direkt ein freies Training und füge die Übungen unterwegs hinzu.</p>
                    <div className="mt-5 flex flex-wrap justify-center gap-2">
                        <Link href="/workouts/new" className={buttonVariants()}><Plus />Vorlage erstellen</Link>
                        <StartButton workoutId={null} className={buttonVariants({ variant: "outline" })}><Play className="fill-current" />Freies Training</StartButton>
                    </div>
                </Card>
            </div>
        </div>
    }

    return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 md:gap-4">
            <div className="flex flex-wrap items-end gap-x-4 gap-y-3">
                <div className="min-w-48 flex-1">
                    <h1 className="text-[22px] font-bold leading-7 tracking-tight">Training</h1>
                    <p className="whitespace-nowrap text-label text-ink">
                        {period === "year"
                            ? `1. Jan – ${formatDate(current[1])} ${current[1].getFullYear()}`
                            : `${formatDate(current[0])} – ${formatDate(current[1])}`}
                    </p>
                </div>
                <Segmented value={period} options={PERIODS} onChange={changePeriod} />
            </div>

            {active && <ActiveBanner session={active} />}

            <Kpis sessions={sessions} inPeriod={inPeriod} period={period} current={current} />

            <div className="grid grid-cols-1 gap-3 md:gap-4 lg:grid-cols-12">
                <Card className="lg:col-span-8">
                    <CardHead title="Vorlagen" right={<Link href="/workouts/new" className="inline-flex items-center gap-1 font-semibold text-brand-strong hover:underline"><Plus className="size-3.5" strokeWidth={2.5} />Neue Vorlage</Link>} />
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

                <Card className="p-3 md:p-4 lg:col-span-12">
                    <CardHead title="Trainings" className="px-1 md:px-0" right={`${inPeriod.length} im Zeitraum`} />
                    <div className="flex flex-col">
                        {inPeriod.slice(0, shown).map((session) =>
                            <SessionRow key={session.id} session={session} expanded={session.id === expanded}
                                onClick={() => setExpanded(session.id === expanded ? null : session.id)} />)}
                        {inPeriod.length > shown && <button onClick={() => setShown(shown + PAGE_SIZE)} className={cn(buttonVariants({ variant: "outline", size: "sm" }), "mt-3 self-start")}>
                            {Math.min(PAGE_SIZE, inPeriod.length - shown)} weitere anzeigen
                        </button>}
                        {!inPeriod.length && <p className="py-6 text-center text-ink-muted">Keine Trainings in diesem Zeitraum</p>}
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
    return <Link href="/workouts/session" className="glass flex items-center gap-3 rounded-[14px] p-3 transition-colors hover:bg-surface-raised/50 md:gap-4 md:p-4">
        <IconTile icon={Dumbbell} solid />
        <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
                <span className="inline-flex h-[22px] items-center gap-1.5 rounded-full bg-success-soft px-2.5 text-label font-semibold text-success">
                    <span className="size-1.5 rounded-full bg-current motion-safe:animate-pulse" />Läuft
                </span>
                <span className="text-caption text-ink-muted">seit {formatTime(new Date(session.startedAt))}</span>
            </div>
            <div className="mt-1 truncate text-heading">{session.name}</div>
            <div className="mt-1.5 flex max-w-xs items-center gap-2 text-caption text-ink-muted">
                <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-track">
                    <div className="h-full rounded-full bg-success-bar" style={{ width: `${sets.length ? done / sets.length * 100 : 0}%` }} />
                </div>
                {done}/{sets.length} Sätze
            </div>
        </div>
        <div className="text-right">
            <div className="text-kpi tabular-nums">{formatDuration(sessionDuration(session, now))}</div>
            <div className="text-caption text-ink-muted">Dauer</div>
        </div>
        <ChevronRight className="size-4 flex-none text-ink-muted" />
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
            label: "Trainings", icon: Dumbbell,
            value: cur.count,
            delta: prev ? <Delta current={cur.count} previous={prev.count} /> : <Note>Ø {de(cur.count / weeks, 1)} / Woche</Note>,
        },
        {
            label: "Zeit", icon: Clock,
            value: <>{time.map((t, i) => i % 2 ? <small key={i}>{t} </small> : <span key={i}>{t} </span>)}</>,
            delta: prev ? <Delta current={cur.time} previous={prev.time} /> : <Note>{cur.count ? `Ø ${formatHours(cur.time / cur.count)} / Training` : " "}</Note>,
        },
        {
            label: "Volumen", icon: Weight,
            value: <>{vol} <small>{volUnit}</small></>,
            delta: prev ? <Delta current={cur.volume} previous={prev.volume} /> : <Note />,
        },
        {
            label: "Sätze", icon: Layers,
            value: cur.sets,
            delta: prev ? <Delta current={cur.sets} previous={prev.sets} /> : <Note>Ø {de(cur.sets / weeks)} / Woche</Note>,
        },
        {
            label: "Wiederholungen", icon: Repeat,
            value: de(cur.reps),
            delta: prev ? <Delta current={cur.reps} previous={prev.reps} /> : <Note />,
        },
    ]} />
}

// MARK: cards


function Templates({ workouts, sessions, exercises, blocked }: { workouts: Workout[], sessions: WorkoutSession[], exercises: Map<number, Exercise>, blocked: boolean }) {
    // a row to swipe through on phones, a grid from sm on
    return <div className="-mx-4 flex snap-x snap-mandatory scroll-px-4 gap-3 overflow-x-auto px-4 pb-1 [scrollbar-width:none] sm:mx-0 sm:grid sm:grid-cols-2 sm:overflow-visible sm:px-0 sm:pb-0 xl:grid-cols-3">
        {workouts.map((workout) => {
            const sets = workout.workoutExercises.reduce((sum, e) => sum + e.sets.length, 0)
            const cardio = workout.workoutExercises.reduce((sum, e) => sum + (e.type === WorkoutExerciseType.CARDIO_EXERCISE ? e.minutes : 0), 0)
            const last = sessions.find((s) => s.workoutId === workout.id)
            const muscles = setsPerMuscle(workout.workoutExercises.map((e) => ({ exerciseId: e.exercise.id, sets: e.sets.length })), exercises)
            return <div key={workout.id} className="flex w-[82%] flex-none snap-start flex-col glass-inset rounded-[12px] p-3 sm:w-auto">
                <div className="flex items-start gap-2">
                    <div className="min-w-0 flex-1">
                        <div className="truncate text-title">{workout.name}</div>
                        <div className="truncate text-caption text-ink-muted">
                            {workout.workoutExercises.length} {workout.workoutExercises.length === 1 ? "Übung" : "Übungen"} · {sets} {sets === 1 ? "Satz" : "Sätze"}
                            {cardio > 0 && ` · ${cardio} min Cardio`}
                            {!workout.editable && workout.ownerName && ` · ${workout.sharedWithMe ? "geteilt von" : "von"} ${workout.ownerName}`}
                            {workout.editable && workout.sharedWith.length > 0 && ` · geteilt mit ${workout.sharedWith.length}`}
                        </div>
                    </div>
                    <Link href={`/workouts/${workout.id}`} aria-label={`${workout.name} ${workout.editable ? "bearbeiten" : "ansehen"}`}
                        className={buttonVariants({ variant: "outline", size: "icon" })}>
                        {workout.editable ? <Pencil /> : <Eye />}
                    </Link>
                </div>
                <div className="mt-2 line-clamp-2 min-h-9">
                    {workout.workoutExercises.map((e) => e.exercise.name).join(" · ") || <span className="text-ink-muted">Keine Übungen</span>}
                </div>
                {muscles.length > 0 && <div className="mt-2 flex flex-wrap gap-1">
                    {muscles.slice(0, 3).map(([slug]) => <span key={slug} className={cn(chip, "bg-brand-soft text-brand-strong")}>{MUSCLES[slug] ?? slug}</span>)}
                </div>}
                <div className="mt-auto flex items-center gap-2 pt-3">
                    <span className="min-w-0 flex-1 truncate text-caption text-ink-muted">{last ? `Zuletzt ${formatDate(new Date(last.startedAt))}` : "Noch nie trainiert"}</span>
                    <StartButton workoutId={workout.id} disabled={blocked} className={buttonVariants({ size: "sm" })}>
                        <Play className="size-3 fill-current" />Starten
                    </StartButton>
                </div>
            </div>
        })}
        <div className="flex min-h-40 w-[82%] flex-none snap-start flex-col items-center justify-center gap-2 rounded-[12px] border border-dashed border-ink-muted/40 p-3 text-center sm:w-auto">
            <StartButton workoutId={null} disabled={blocked} className={buttonVariants({ variant: "outline", size: "sm" })}>
                <Play className="size-3 fill-current" />Freies Training
            </StartButton>
            <Link href="/workouts/new" className="flex items-center gap-1 py-1 text-label font-semibold text-brand-strong hover:underline"><Plus className="size-3.5" strokeWidth={2.5} />Vorlage erstellen</Link>
        </div>
    </div>
}

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
    if (!rows.length) return <p className="py-6 text-ink-muted">Noch keine Sätze mit Gewicht in {year}</p>

    return <div className="flex flex-col">
        {rows.map((row, i) => <button key={row.name + i} onClick={() => onSelect(row.session)}
            className={cn("-mx-2 flex items-center gap-3 rounded-[8px] px-2 py-2 text-left hover:bg-surface-raised/50 dark:hover:bg-white/5", i > 0 && "border-t border-line")}>
            <IconTile icon={Trophy} size="sm" />
            <div className="min-w-0 flex-1">
                <div className="truncate font-semibold">{row.name}</div>
                <div className="text-caption text-ink-muted">{formatDate(new Date(row.session.startedAt))} · {formatSet(row)}</div>
            </div>
            <div className="text-right">
                <div className="text-title font-bold tabular-nums">{formatWeight(Math.round(row.max))} kg</div>
                <div className="text-caption text-ink-muted">1RM geschätzt</div>
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

    return <div id={`session-${session.id}`} className={cn("rounded-[12px] border-b border-line last:border-b-0", expanded && "border-transparent bg-brand-soft/60")}>
        <button onClick={onClick} aria-expanded={expanded} className={cn(
            "grid w-full grid-cols-[40px_minmax(0,1fr)_auto] items-center gap-3 rounded-[12px] px-2 py-2 text-left md:gap-4 md:px-3 md:grid-cols-[40px_minmax(0,1.4fr)_repeat(4,minmax(0,1fr))_16px]",
            !expanded && "hover:bg-surface-raised/50 dark:hover:bg-white/5")}>
            <div className={cn("grid size-10 place-items-center rounded-[10px] transition-colors", expanded ? "bg-brand text-on-brand" : "bg-brand-soft text-brand")}>
                <Dumbbell className="size-5" strokeWidth={2} />
            </div>
            <div className="min-w-0">
                <div className="truncate font-bold">{session.name}</div>
                <div className="truncate text-caption text-ink-muted">{formatDate(start)} · {formatTime(start)} Uhr</div>
            </div>
            <Cell value={formatDuration(sessionDuration(session))} label="Zeit" />
            <Cell value={`${session.exercises.length}`} label="Übungen" hide />
            <Cell value={`${sets.length}`} label="Sätze" hide />
            <Cell value={vol} label={volUnit} hide />
            <ChevronRight className={cn("hidden size-4 text-ink-muted transition-transform md:block", expanded && "rotate-90")} />
        </button>
        {expanded && <div className="px-2 pb-3 md:px-3 md:pl-[72px]">
            <div className="flex flex-col glass-inset divide-y divide-line rounded-[10px]">
                {session.exercises.map((exercise, i) => <div key={i} className="flex flex-col gap-1.5 px-3 py-2.5 md:flex-row md:items-center md:gap-4">
                    <div className="md:w-56 md:flex-none">
                        <div className="font-semibold">{exercise.name}</div>
                        {exercise.note && <div className="text-caption text-ink-muted">{exercise.note}</div>}
                    </div>
                    <div className="flex flex-wrap gap-1">
                        {exercise.cardio && <span className={cn(chip, "bg-brand-soft tabular-nums text-brand-strong")}>{exercise.cardio.minutes} min</span>}
                        {exercise.sets.filter((s) => s.done).map((set, j) =>
                            <span key={j} className={cn(chip, neutralChip, "tabular-nums")}>{formatSet(set)}</span>)}
                    </div>
                </div>)}
                {!session.exercises.length && <p className="px-3 py-4 text-ink-muted">Keine Sätze erfasst</p>}
            </div>
            <div className="mt-2 flex justify-end">
                <button onClick={remove} disabled={deleting} className={buttonVariants({ variant: "destructive", size: "sm" })}>
                    <Trash2 className="size-3.5" />{deleting ? "Löscht…" : "Training löschen"}
                </button>
            </div>
        </div>}
    </div>
}

function Cell({ value, label, hide }: { value: string, label: string, hide?: boolean }) {
    return <div className={cn("font-semibold tabular-nums", hide && "hidden md:block")}>
        {value}<small className="block text-caption font-medium text-ink-muted">{label}</small>
    </div>
}

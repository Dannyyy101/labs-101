'use client'

import { Card, CardHead, IconTile, Stat } from "@/components/dashboard"
import { de, formatHours, km } from "@/app/runs/format"
import { addDays, formatDayTitle, formatLongDay, formatNumber, today } from "@/app/foods/track/format"
import { cn } from "@/lib/utils"
import { HealthOverview, Sleep } from "@/utils/types/overview"
import { ChevronLeft, ChevronRight, Dumbbell, Flame, Footprints, type LucideIcon, Moon, TrendingDown, TrendingUp, Zap } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { HeartRateChart, Hypnogram, ScoreRing, STAGES, TrendBars } from "./Charts"
import { formatClock, formatSleep, percent, workoutName } from "./format"

// every chart shows one measure and is named by its card, so they all use the brand blue
const BRAND = "var(--brand)"

const ZONES = [
    { key: "Z1", name: "Sehr leicht", color: "var(--ramp-1)" },
    { key: "Z2", name: "Leicht", color: "var(--ramp-2)" },
    { key: "Z3", name: "Moderat", color: "var(--ramp-3)" },
    { key: "Z4", name: "Hart", color: "var(--ramp-4)" },
    { key: "Z5", name: "Maximal", color: "var(--ramp-5)" },
]

const GOALS = { steps: 10000, exercise: 30, stand: 12 }

const recoveryColor = (score: number | null) =>
    score == null ? "var(--track)" : score >= 67 ? "var(--success-bar)" : score >= 34 ? "var(--warning)" : "var(--danger)"

function recoveryStatus(score: number | null) {
    if (score == null) return "Noch zu wenige Nächte zum Vergleichen"
    if (score >= 67) return "Bereit für hohe Belastung"
    if (score >= 34) return "Moderat belastbar"
    return "Heute auf Erholung achten"
}

// how much strain the recovery allows, like Bevel's strain target
function strainTarget(recovery: number | null): [number, number] | null {
    if (recovery == null) return null
    if (recovery >= 67) return [55, 80]
    if (recovery >= 34) return [35, 60]
    return [10, 35]
}

function strainStatus(score: number) {
    if (score < 15) return "Ruhetag"
    if (score < 40) return "Leichte Belastung"
    if (score < 65) return "Moderate Belastung"
    return "Hohe Belastung"
}

function sleepStatus(sleep: Sleep | null) {
    if (!sleep) return "Keine Schlafdaten"
    if (sleep.score >= 85) return "Erholsamer Schlaf"
    if (sleep.score >= 70) return "Guter Schlaf"
    if (sleep.score >= 50) return "Etwas zu wenig Schlaf"
    return "Schlechter Schlaf"
}

export default function HealthHome({ overview }: { overview: HealthOverview }) {
    const router = useRouter()
    const { date: day, sleep, recovery, strain, activity, history } = overview
    const isToday = day >= today()
    const open = (d: string) => router.push(d === today() ? "/" : `/?date=${d}`)
    const hasData = history.some((d) => d.sleep != null || d.steps > 0 || d.strain != null)
    const target = strainTarget(recovery.score)

    return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 md:gap-4">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-56 flex-1">
                    <h1 className="text-[22px] font-bold leading-7 tracking-tight">{formatDayTitle(day)}</h1>
                    <p className="text-label text-ink">{formatLongDay(day)}</p>
                </div>
                <div className="glass flex items-center gap-0.5 rounded-[12px] p-[3px] text-label font-semibold">
                    <Link href={`/?date=${addDays(day, -1)}`} aria-label="Vorheriger Tag" className="grid size-8 place-items-center rounded-[9px] text-ink-muted hover:bg-surface-raised/60 hover:text-ink"><ChevronLeft className="size-4" /></Link>
                    <Link href="/" className={cn("flex h-8 items-center rounded-[9px] px-3.5", isToday ? "bg-surface-raised text-ink shadow-xs dark:bg-white/15" : "text-ink-muted hover:text-ink")}>Heute</Link>
                    {isToday
                        ? <span className="grid size-8 place-items-center text-ink-muted opacity-40"><ChevronRight className="size-4" /></span>
                        : <Link href={`/?date=${addDays(day, 1)}`} aria-label="Nächster Tag" className="grid size-8 place-items-center rounded-[9px] text-ink-muted hover:bg-surface-raised/60 hover:text-ink"><ChevronRight className="size-4" /></Link>}
                </div>
            </div>

            {!hasData && <Card className="p-10 text-center">
                <p className="text-heading">Noch keine Gesundheitsdaten</p>
                <p className="mt-1 text-ink-muted">Die iOS-App überträgt Schlaf, Herzfrequenz und Aktivität aus Apple Health, sobald sie synchronisiert hat.</p>
            </Card>}

            {/* the three scores */}
            <div className="grid grid-cols-1 gap-3 md:grid-cols-3 md:gap-4">
                <ScoreCard title="Erholung" score={recovery.score} color={recoveryColor(recovery.score)} unit="%"
                    status={recoveryStatus(recovery.score)}
                    facts={[
                        ["HRV", recovery.hrv != null ? `${Math.round(recovery.hrv)} ms` : "–"],
                        ["Ruhepuls", recovery.restingHeartRate != null ? `${Math.round(recovery.restingHeartRate)} bpm` : "–"],
                    ]} />
                <ScoreCard title="Belastung" score={strain.score} color={BRAND} unit="%"
                    status={strainStatus(strain.score)}
                    facts={[
                        ["Ziel", target ? `${target[0]}–${target[1]} %` : "–"],
                        ["Aktiv", `${formatNumber(activity.activeEnergy)} kcal`],
                    ]} />
                <ScoreCard title="Schlaf" score={sleep?.score ?? null} color={BRAND}
                    status={sleepStatus(sleep)}
                    facts={[
                        ["Dauer", sleep ? formatSleep(sleep.asleep) : "–"],
                        ["Bedarf", sleep ? `${percent(Math.min(sleep.asleep / sleep.need, 9.99))}` : "–"],
                    ]} />
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <Card className="lg:col-span-8">
                    <CardHead title="Schlaf" right={sleep ? `${formatClock(sleep.start)} – ${formatClock(sleep.end)}` : undefined} />
                    {sleep ? <SleepDetail sleep={sleep} /> : <Empty icon={<Moon />} text="Für diese Nacht wurde kein Schlaf aufgezeichnet." />}
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Vitalwerte" right="vs. Ø 30 Tage" />
                    <div className="divide-y divide-line">
                        <Vital label="Herzfrequenzvariabilität" value={recovery.hrv} baseline={recovery.hrvBaseline} unit="ms" higherIsBetter />
                        <Vital label="Ruhepuls" value={recovery.restingHeartRate} baseline={recovery.restingHeartRateBaseline} unit="bpm" />
                        <Vital label="Atemfrequenz" value={recovery.respiratoryRate} baseline={recovery.respiratoryRateBaseline} unit="/min" decimals={1} neutral />
                        <Vital label="Blutsauerstoff" value={recovery.oxygenSaturation != null ? recovery.oxygenSaturation * 100 : null} unit="%" decimals={1} />
                        <Vital label="Handgelenktemperatur" value={recovery.wristTemperatureDeviation} unit="°C" decimals={2} signed />
                    </div>
                </Card>

                <Card className="lg:col-span-8">
                    <CardHead title="Herzfrequenz" right={strain.averageHeartRate != null
                        ? `Ø ${Math.round(strain.averageHeartRate)} · max ${Math.round(strain.peakHeartRate ?? 0)} bpm`
                        : undefined} />
                    <HeartRateChart points={overview.heartRate} day={day} resting={recovery.restingHeartRate} color={BRAND} />
                    <Zones seconds={strain.zoneSeconds} max={strain.maxHeartRate} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Aktivität" />
                    <div className="flex flex-col gap-3.5">
                        <Goal icon={Footprints} label="Schritte" value={activity.steps} goal={GOALS.steps} format={formatNumber} />
                        <Goal icon={Dumbbell} label="Trainingsminuten" value={activity.exerciseMinutes} goal={GOALS.exercise} format={(v) => `${Math.round(v)} min`} />
                        <Goal icon={Zap} label="Stehstunden" value={activity.standHours} goal={GOALS.stand} format={(v) => `${v} h`} />
                    </div>
                    <dl className="mt-4 grid grid-cols-2 gap-x-3 gap-y-4 border-t border-line pt-4">
                        <Stat label="Aktive Energie" value={formatNumber(activity.activeEnergy)} unit="kcal" />
                        <Stat label="Gesamt" value={formatNumber(activity.activeEnergy + activity.basalEnergy)} unit="kcal" />
                        <Stat label="Distanz" value={km(activity.distance)} unit="km" />
                        <Stat label="Stockwerke" value={formatNumber(activity.flightsClimbed)} />
                        <Stat label="Tageslicht" value={formatHours(activity.daylightMinutes * 60)} />
                        <Stat label="Gegessen" value={activity.energyConsumed != null ? formatNumber(activity.energyConsumed) : "–"} unit={activity.energyConsumed != null ? "kcal" : undefined} />
                    </dl>
                </Card>
            </div>

            <div>
                <h2 className="mb-3 text-heading">Verlauf</h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:gap-4 lg:grid-cols-4">
                    <Card>
                        <CardHead title="Erholung" small />
                        <TrendBars history={history} day={day} value={(d) => d.recovery} color="var(--success-bar)" format={(v) => `${Math.round(v)} %`} max={100} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="Belastung" small />
                        <TrendBars history={history} day={day} value={(d) => d.strain} color={BRAND} format={(v) => `${Math.round(v)} %`} max={100} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="Schlaf" small />
                        <TrendBars history={history} day={day} value={(d) => d.sleep} color={BRAND} format={formatSleep} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="HRV" small />
                        <TrendBars history={history} day={day} value={(d) => d.hrv} color={BRAND} format={(v) => `${Math.round(v)} ms`} onSelect={open} />
                    </Card>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <Card className="lg:col-span-8">
                    <CardHead title="Trainings" right={overview.workouts.length ? `${overview.workouts.length}` : undefined} />
                    {overview.workouts.length
                        ? <ul className="divide-y divide-line">
                            {overview.workouts.map((w) => <li key={w.id} className="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0">
                                <IconTile icon={Flame} size="sm" />
                                <div className="min-w-0 flex-1">
                                    <div className="font-semibold">{w.activityType === 37 ? <Link href="/runs" className="hover:underline">{workoutName(w.activityType)}</Link> : workoutName(w.activityType)}</div>
                                    <div className="text-caption text-ink-muted">{formatClock(w.startDate)} – {formatClock(w.endDate)}</div>
                                </div>
                                <div className="text-right tabular-nums">
                                    <div className="font-semibold">{formatSleep(w.duration)}</div>
                                    <div className="text-caption text-ink-muted">
                                        {[w.distance ? `${km(w.distance, 2)} km` : null,
                                        w.energy ? `${formatNumber(w.energy)} kcal` : null,
                                        w.averageHeartRate ? `Ø ${Math.round(w.averageHeartRate)} bpm` : null].filter(Boolean).join(" · ")}
                                    </div>
                                </div>
                            </li>)}
                        </ul>
                        : <Empty icon={<Dumbbell />} text="Kein Training an diesem Tag." />}
                </Card>
                <Card className="lg:col-span-4">
                    <CardHead title="Körper" />
                    <dl className="grid grid-cols-2 gap-x-3 gap-y-4">
                        <Stat label="VO₂max" value={overview.vitals.vo2Max != null ? de(overview.vitals.vo2Max, 1) : "–"} unit={overview.vitals.vo2Max != null ? "ml/kg·min" : undefined} />
                        <div>
                            <Stat label="Gewicht" value={overview.vitals.bodyMass != null ? de(overview.vitals.bodyMass, 1) : "–"} unit={overview.vitals.bodyMass != null ? "kg" : undefined} />
                            {overview.vitals.bodyMassChange != null && Math.abs(overview.vitals.bodyMassChange) >= 0.05 && <div className="mt-0.5 text-caption text-ink-muted tabular-nums">
                                {overview.vitals.bodyMassChange > 0 ? "+" : "−"}{de(Math.abs(overview.vitals.bodyMassChange), 1)} kg in 30 Tagen
                            </div>}
                        </div>
                    </dl>
                </Card>
            </div>
        </div>
    </div>
}

function ScoreCard({ title, score, color, unit, status, facts }: { title: string, score: number | null, color: string, unit?: string, status: string, facts: [string, string][] }) {
    return <Card className="flex items-center gap-4">
        <ScoreRing value={score} color={color} size={108} stroke={10}>
            <span className="text-[28px] font-bold leading-none tracking-tight tabular-nums">{score ?? "–"}{score != null && unit && <small className="text-body font-semibold text-ink-muted">{unit}</small>}</span>
        </ScoreRing>
        <div className="min-w-0 flex-1">
            <div className="text-label text-ink-muted">{title}</div>
            <div className="mt-0.5 text-heading">{status}</div>
            <dl className="mt-2.5 flex gap-5 text-label">
                {facts.map(([label, value]) => <div key={label}>
                    <dt className="text-ink-muted">{label}</dt>
                    <dd className="font-semibold tabular-nums">{value}</dd>
                </div>)}
            </dl>
        </div>
    </Card>
}

function SleepDetail({ sleep }: { sleep: Sleep }) {
    const staged = sleep.deep + sleep.core + sleep.rem
    const parts = [
        { ...STAGES[0], seconds: sleep.awake },
        { ...STAGES[1], seconds: sleep.rem },
        { ...STAGES[2], seconds: sleep.core },
        { ...STAGES[3], seconds: sleep.deep },
    ]
    const total = parts.reduce((s, p) => s + p.seconds, 0)

    return <>
        <dl className="mb-4 grid grid-cols-2 gap-x-3 gap-y-4 sm:grid-cols-4">
            <Stat label="Geschlafen" value={formatSleep(sleep.asleep)} />
            <Stat label="Im Bett" value={formatSleep(sleep.inBed)} />
            <Stat label="Effizienz" value={sleep.inBed ? percent(Math.min(1, (sleep.inBed - sleep.awake) / sleep.inBed)) : "–"} />
            <Stat label="Atemfrequenz" value={sleep.respiratoryRate != null ? de(sleep.respiratoryRate, 1) : "–"} unit={sleep.respiratoryRate != null ? "/min" : undefined} />
        </dl>
        <Hypnogram sleep={sleep} />
        {staged > 0 && total > 0 && <>
            <div className="mt-4 flex h-2 gap-0.5 overflow-hidden rounded-full">
                {parts.map((p) => <div key={p.stage} style={{ width: `${p.seconds / total * 100}%`, background: p.color }} />)}
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {parts.map((p) => <div key={p.stage}>
                    <div className="flex items-center gap-1.5 text-label text-ink-muted">
                        <span className="size-2 rounded-full" style={{ background: p.color }} />{p.label}
                    </div>
                    <div className="font-semibold tabular-nums">{formatSleep(p.seconds)} <span className="text-caption font-medium text-ink-muted">{percent(p.seconds / total)}</span></div>
                </div>)}
            </div>
        </>}
    </>
}

function Vital({ label, value, baseline, unit, decimals = 0, higherIsBetter, neutral, signed }: {
    label: string, value: number | null, baseline?: number | null, unit: string, decimals?: number,
    higherIsBetter?: boolean, neutral?: boolean, signed?: boolean
}) {
    const change = value != null && baseline ? (value - baseline) / baseline : null
    const good = change != null && (higherIsBetter ? change > 0 : change < 0)
    return <div className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
        <div className="min-w-0">
            <div className="text-label text-ink-muted">{label}</div>
            <div className="text-[20px] font-bold leading-6 tabular-nums">
                {value != null ? `${signed && value > 0 ? "+" : ""}${de(value, decimals)}` : "–"}
                {value != null && <small className="ml-1 text-label text-ink-muted">{unit}</small>}
            </div>
        </div>
        {baseline != null && <div className="text-right text-label tabular-nums">
            {change != null && Math.abs(change) >= 0.02 && <div className={cn("flex items-center justify-end gap-1 font-semibold",
                neutral ? "text-ink-muted" : good ? "text-success" : "text-danger")}>
                {change > 0 ? <TrendingUp className="size-3.5" /> : <TrendingDown className="size-3.5" />}{percent(Math.abs(change))}
            </div>}
            <div className="text-ink-muted">Ø {de(baseline, decimals)}</div>
        </div>}
    </div>
}

function Zones({ seconds, max }: { seconds: number[], max: number }) {
    const total = seconds.reduce((a, b) => a + b, 0)
    return <div className="mt-4 border-t border-line pt-4">
        <div className="mb-2.5 flex items-baseline justify-between text-label">
            <span className="text-title">Herzfrequenzzonen</span>
            <span className="text-ink-muted">max. {Math.round(max)} bpm</span>
        </div>
        <div className="grid grid-cols-5 gap-2">
            {ZONES.map((z, i) => <div key={z.key}>
                <div className="h-1.5 rounded-full" style={{ background: z.color, opacity: total && seconds[i] ? 1 : .25 }} />
                <div className="mt-1.5 text-caption text-ink-muted">{z.key} · {Math.round(max * (0.5 + i * 0.1))}+</div>
                <div className="font-semibold tabular-nums">{seconds[i] >= 60 ? formatSleep(seconds[i]) : "–"}</div>
            </div>)}
        </div>
    </div>
}

function Goal({ icon, label, value, goal, format }: { icon: LucideIcon, label: string, value: number, goal: number, format: (v: number) => string }) {
    return <div className="flex items-center gap-3">
        <IconTile icon={icon} size="sm" />
        <div className="min-w-0 flex-1">
            <div className="flex items-baseline justify-between text-label">
                <span className="text-ink-muted">{label}</span>
                <span className="tabular-nums"><span className="font-semibold">{format(value)}</span> <span className="text-ink-muted">/ {format(goal)}</span></span>
            </div>
            <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-track">
                <div className={cn("h-full rounded-full", value >= goal ? "bg-success-bar" : "bg-brand")} style={{ width: `${Math.min(100, value / goal * 100)}%` }} />
            </div>
        </div>
    </div>
}

function Empty({ icon, text }: { icon: React.ReactNode, text: string }) {
    return <div className="flex flex-col items-center justify-center gap-2 py-10 text-center text-ink-muted [&_svg]:size-6 [&_svg]:text-brand">
        {icon}<p>{text}</p>
    </div>
}

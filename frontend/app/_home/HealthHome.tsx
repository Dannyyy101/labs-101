'use client'

import { Card, CardHead, Stat } from "@/components/dashboard"
import { de, formatHours, km } from "@/app/runs/format"
import { addDays, formatDayTitle, formatLongDay, formatNumber, today } from "@/app/foods/track/format"
import { cn } from "@/lib/utils"
import { HealthOverview, Sleep } from "@/utils/types/overview"
import { ChevronLeft, ChevronRight, Dumbbell, Flame, Footprints, Moon, TrendingDown, TrendingUp, Zap } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { HeartRateChart, Hypnogram, ScoreRing, STAGES, TrendBars } from "./Charts"
import { formatClock, formatSleep, percent, workoutName } from "./format"

const COLORS = { strain: "#ff9f0a", sleep: "#5e5ce6", heart: "#ff375f", hrv: "#30d158" }

const ZONES = [
    { key: "Z1", name: "Sehr leicht", color: "#64d2ff" },
    { key: "Z2", name: "Leicht", color: "#30d158" },
    { key: "Z3", name: "Moderat", color: "#ffd60a" },
    { key: "Z4", name: "Hart", color: "#ff9f0a" },
    { key: "Z5", name: "Maximal", color: "#ff453a" },
]

const GOALS = { steps: 10000, exercise: 30, stand: 12 }

const recoveryColor = (score: number | null) =>
    score == null ? "#8e8e93" : score >= 67 ? "#30d158" : score >= 34 ? "#ffd60a" : "#ff453a"

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

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-56 flex-1">
                    <h1 className="text-[34px] font-bold leading-tight tracking-tight">{formatDayTitle(day)}</h1>
                    <p className="text-muted-foreground">{formatLongDay(day)}</p>
                </div>
                <div className="flex items-center gap-1 rounded-[9px] bg-muted p-0.5 text-sm font-medium">
                    <Link href={`/?date=${addDays(day, -1)}`} aria-label="Vorheriger Tag" className="rounded-[7px] p-1.5 hover:bg-background"><ChevronLeft className="size-4" /></Link>
                    <Link href="/" className={cn("rounded-[7px] px-3 py-1", isToday ? "bg-background font-semibold shadow-[0_1px_3px_rgba(0,0,0,.12)]" : "text-muted-foreground")}>Heute</Link>
                    {isToday
                        ? <span className="p-1.5 opacity-30"><ChevronRight className="size-4" /></span>
                        : <Link href={`/?date=${addDays(day, 1)}`} aria-label="Nächster Tag" className="rounded-[7px] p-1.5 hover:bg-background"><ChevronRight className="size-4" /></Link>}
                </div>
            </div>

            {!hasData && <Card className="p-10 text-center">
                <p className="text-lg font-semibold">Noch keine Gesundheitsdaten</p>
                <p className="mt-1 text-muted-foreground">Die iOS-App überträgt Schlaf, Herzfrequenz und Aktivität aus Apple Health, sobald sie synchronisiert hat.</p>
            </Card>}

            {/* the three scores */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <ScoreCard title="Erholung" score={recovery.score} color={recoveryColor(recovery.score)} unit="%"
                    status={recoveryStatus(recovery.score)}
                    facts={[
                        ["HRV", recovery.hrv != null ? `${Math.round(recovery.hrv)} ms` : "–"],
                        ["Ruhepuls", recovery.restingHeartRate != null ? `${Math.round(recovery.restingHeartRate)} bpm` : "–"],
                    ]} />
                <ScoreCard title="Belastung" score={strain.score} color={COLORS.strain} unit="%"
                    status={strainStatus(strain.score)}
                    facts={[
                        ["Ziel", target ? `${target[0]}–${target[1]} %` : "–"],
                        ["Aktiv", `${formatNumber(activity.activeEnergy)} kcal`],
                    ]} />
                <ScoreCard title="Schlaf" score={sleep?.score ?? null} color={COLORS.sleep}
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
                    <div className="divide-y">
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
                    <HeartRateChart points={overview.heartRate} day={day} resting={recovery.restingHeartRate} color={COLORS.heart} />
                    <Zones seconds={strain.zoneSeconds} max={strain.maxHeartRate} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Aktivität" />
                    <div className="flex flex-col gap-3.5">
                        <Goal icon={<Footprints />} label="Schritte" value={activity.steps} goal={GOALS.steps} color="#30d158" format={formatNumber} />
                        <Goal icon={<Dumbbell />} label="Trainingsminuten" value={activity.exerciseMinutes} goal={GOALS.exercise} color="#a3f800" format={(v) => `${Math.round(v)} min`} />
                        <Goal icon={<Zap />} label="Stehstunden" value={activity.standHours} goal={GOALS.stand} color="#64d2ff" format={(v) => `${v} h`} />
                    </div>
                    <dl className="mt-4 grid grid-cols-2 gap-x-3 gap-y-4 border-t pt-4">
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
                <h2 className="mb-3 text-[22px] font-bold tracking-tight">Verlauf</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <Card>
                        <CardHead title="Erholung" small />
                        <TrendBars history={history} day={day} value={(d) => d.recovery} color="#30d158" format={(v) => `${Math.round(v)} %`} max={100} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="Belastung" small />
                        <TrendBars history={history} day={day} value={(d) => d.strain} color={COLORS.strain} format={(v) => `${Math.round(v)} %`} max={100} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="Schlaf" small />
                        <TrendBars history={history} day={day} value={(d) => d.sleep} color={COLORS.sleep} format={formatSleep} onSelect={open} />
                    </Card>
                    <Card>
                        <CardHead title="HRV" small />
                        <TrendBars history={history} day={day} value={(d) => d.hrv} color={COLORS.hrv} format={(v) => `${Math.round(v)} ms`} onSelect={open} />
                    </Card>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <Card className="lg:col-span-8">
                    <CardHead title="Trainings" right={overview.workouts.length ? `${overview.workouts.length}` : undefined} />
                    {overview.workouts.length
                        ? <ul className="divide-y">
                            {overview.workouts.map((w) => <li key={w.id} className="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0">
                                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#ff9f0a]/15 text-[#ff9f0a]"><Flame className="size-5" /></div>
                                <div className="min-w-0 flex-1">
                                    <div className="font-semibold">{w.activityType === 37 ? <Link href="/runs" className="hover:underline">{workoutName(w.activityType)}</Link> : workoutName(w.activityType)}</div>
                                    <div className="text-sm text-muted-foreground">{formatClock(w.startDate)} – {formatClock(w.endDate)}</div>
                                </div>
                                <div className="text-right text-sm tabular-nums">
                                    <div className="font-semibold">{formatSleep(w.duration)}</div>
                                    <div className="text-muted-foreground">
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
                            {overview.vitals.bodyMassChange != null && Math.abs(overview.vitals.bodyMassChange) >= 0.05 && <div className="mt-0.5 text-sm text-muted-foreground tabular-nums">
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
    return <Card className="flex items-center gap-5">
        <ScoreRing value={score} color={color} size={116} stroke={11}>
            <span className="text-[30px] font-bold leading-none tracking-tight tabular-nums">{score ?? "–"}{score != null && unit && <small className="text-base font-semibold text-muted-foreground">{unit}</small>}</span>
        </ScoreRing>
        <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
                <span className="size-2 rounded-full" style={{ background: color }} />{title}
            </div>
            <div className="mt-0.5 text-[17px] font-semibold leading-snug">{status}</div>
            <dl className="mt-2.5 flex gap-5 text-sm">
                {facts.map(([label, value]) => <div key={label}>
                    <dt className="text-muted-foreground">{label}</dt>
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
            <div className="mt-4 flex h-2.5 overflow-hidden rounded-full">
                {parts.map((p) => <div key={p.stage} style={{ width: `${p.seconds / total * 100}%`, background: p.color }} />)}
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {parts.map((p) => <div key={p.stage}>
                    <div className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
                        <span className="size-2 rounded-full" style={{ background: p.color }} />{p.label}
                    </div>
                    <div className="font-semibold tabular-nums">{formatSleep(p.seconds)} <span className="text-sm font-normal text-muted-foreground">{percent(p.seconds / total)}</span></div>
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
            <div className="text-sm font-medium text-muted-foreground">{label}</div>
            <div className="text-xl font-semibold tabular-nums">
                {value != null ? `${signed && value > 0 ? "+" : ""}${de(value, decimals)}` : "–"}
                {value != null && <small className="ml-1 text-sm font-medium text-muted-foreground">{unit}</small>}
            </div>
        </div>
        {baseline != null && <div className="text-right text-sm tabular-nums">
            {change != null && Math.abs(change) >= 0.02 && <div className={cn("flex items-center justify-end gap-1 font-medium",
                neutral ? "text-muted-foreground" : good ? "text-[#30d158]" : "text-[#ff453a]")}>
                {change > 0 ? <TrendingUp className="size-3.5" /> : <TrendingDown className="size-3.5" />}{percent(Math.abs(change))}
            </div>}
            <div className="text-muted-foreground">Ø {de(baseline, decimals)}</div>
        </div>}
    </div>
}

function Zones({ seconds, max }: { seconds: number[], max: number }) {
    const total = seconds.reduce((a, b) => a + b, 0)
    return <div className="mt-4 border-t pt-4">
        <div className="mb-2.5 flex items-baseline justify-between text-sm">
            <span className="font-semibold">Herzfrequenzzonen</span>
            <span className="text-muted-foreground">max. {Math.round(max)} bpm</span>
        </div>
        <div className="grid grid-cols-5 gap-2">
            {ZONES.map((z, i) => <div key={z.key}>
                <div className="h-1.5 rounded-full" style={{ background: z.color, opacity: total && seconds[i] ? 1 : .25 }} />
                <div className="mt-1.5 text-xs text-muted-foreground">{z.key} · {Math.round(max * (0.5 + i * 0.1))}+</div>
                <div className="text-sm font-semibold tabular-nums">{seconds[i] >= 60 ? formatSleep(seconds[i]) : "–"}</div>
            </div>)}
        </div>
    </div>
}

function Goal({ icon, label, value, goal, color, format }: { icon: React.ReactNode, label: string, value: number, goal: number, color: string, format: (v: number) => string }) {
    return <div className="flex items-center gap-3">
        <div className="flex size-9 shrink-0 items-center justify-center rounded-full [&_svg]:size-[18px]" style={{ background: `${color}26`, color }}>{icon}</div>
        <div className="min-w-0 flex-1">
            <div className="flex items-baseline justify-between text-sm">
                <span className="font-medium text-muted-foreground">{label}</span>
                <span className="tabular-nums"><span className="font-semibold">{format(value)}</span> <span className="text-muted-foreground">/ {format(goal)}</span></span>
            </div>
            <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full" style={{ width: `${Math.min(100, value / goal * 100)}%`, background: color }} />
            </div>
        </div>
    </div>
}

function Empty({ icon, text }: { icon: React.ReactNode, text: string }) {
    return <div className="flex flex-col items-center justify-center gap-2 py-10 text-center text-muted-foreground [&_svg]:size-7 [&_svg]:opacity-50">
        {icon}<p>{text}</p>
    </div>
}

'use client'

import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { RunDetail, RunPoint, RunSummary } from "@/utils/types/run"
import { ArrowRight, ChevronRight, Clock, Footprints, Gauge, type LucideIcon, Mountain, Route, Trophy, Zap } from "lucide-react"
import { useMemo, useRef, useState, useTransition } from "react"
import { getRun } from "./action"
import { Card, CardHead, Delta, IconTile, KpiGrid, Note, Period, PERIODS, periodRange, isInPeriod, Segmented, Stat } from "@/components/dashboard"
import { ActivityChart, ProfileChart, WEEKLY_GOAL_KM } from "./Charts"
import { de, formatDate, formatDuration, formatHours, formatMonth, formatPace, km, paceColor } from "./format"
import RunMap from "./RunMap"


const ZONES = [
    { key: "Z1", name: "Regeneration", bpm: "< 135", color: "var(--ramp-1)" },
    { key: "Z2", name: "Grundlage", bpm: "135–147", color: "var(--ramp-2)" },
    { key: "Z3", name: "Aerob", bpm: "148–159", color: "var(--ramp-3)" },
    { key: "Z4", name: "Schwelle", bpm: "160–170", color: "var(--ramp-4)" },
    { key: "Z5", name: "Maximal", bpm: "> 170", color: "var(--ramp-5)" },
]

const PAGE_SIZE = 8

const inRange = (run: RunSummary, range: [Date, Date]) => isInPeriod(run.startDate, range)

function aggregate(runs: RunSummary[]) {
    const distance = runs.reduce((sum, r) => sum + r.distance, 0)
    const time = runs.reduce((sum, r) => sum + r.duration, 0)
    return {
        count: runs.length,
        distance,
        time,
        pace: distance ? time / (distance / 1000) : 0,
        elevation: runs.reduce((sum, r) => sum + (r.elevationGain ?? 0), 0),
    }
}

const pace = (run: { duration: number, distance: number }) => run.distance ? run.duration / (run.distance / 1000) : 0

export default function RunDashboard({ runs, initialDetail }: { runs: RunSummary[], initialDetail: RunDetail | null }) {
    const [period, setPeriod] = useState<Period>("30")
    const [selected, setSelected] = useState<RunSummary | null>(initialDetail?.summary ?? null)
    const [detail, setDetail] = useState<RunDetail | null>(initialDetail)
    const [shown, setShown] = useState(PAGE_SIZE)
    const [highlightKm, setHighlightKm] = useState<number | null>(null)
    const [cursor, setCursor] = useState<RunPoint | null>(null)
    const [loading, startLoading] = useTransition()
    // a later click wins over a slower response of an earlier one
    const requested = useRef(initialDetail?.summary.id)

    const current = useMemo(() => periodRange(period), [period])
    const inPeriod = useMemo(() => runs.filter((r) => inRange(r, current)), [runs, current])

    const select = (run: RunSummary) => {
        setSelected(run)
        setHighlightKm(null)
        setCursor(null)
        requested.current = run.id
        startLoading(async () => {
            const next = await getRun(run.id)
            if (requested.current === run.id) setDetail(next)
        })
    }

    const changePeriod = (value: Period) => {
        setPeriod(value)
        setShown(PAGE_SIZE)
    }

    if (!runs.length) {
        return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
            <div className="mx-auto max-w-7xl">
                <h1 className="text-[22px] font-bold leading-7 tracking-tight">Laufen</h1>
                <Card className="mt-5 flex flex-col items-center p-10 text-center">
                    <IconTile icon={Footprints} />
                    <p className="mt-4 text-heading">Noch keine Läufe</p>
                    <p className="mt-1 text-ink-muted">Die iOS-App überträgt deine Läufe aus Apple Health, sobald sie synchronisiert hat.</p>
                </Card>
            </div>
        </div>
    }

    const shownDetail = detail && detail.summary.id === selected?.id ? detail : null

    return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 md:gap-4">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-56 flex-1">
                    <h1 className="text-[22px] font-bold leading-7 tracking-tight">Laufen</h1>
                    <p className="whitespace-nowrap text-label text-ink">
                        {period === "year"
                            ? `1. Jan – ${formatDate(current[1])} ${current[1].getFullYear()}`
                            : `${formatDate(current[0])} – ${formatDate(current[1])}`}
                    </p>
                </div>
                <Segmented value={period} options={PERIODS} onChange={changePeriod} />
            </div>

            <Kpis runs={runs} inPeriod={inPeriod} period={period} current={current} />

            <div className="grid grid-cols-1 gap-3 md:gap-4 lg:grid-cols-12">
                <Card className="relative p-1 lg:col-span-8">
                    <RunMap points={shownDetail?.points ?? []} highlightKm={highlightKm} cursor={cursor}>
                        {selected && <div className="glass absolute left-4 top-4 z-10 max-w-[calc(100%-2rem)] rounded-[14px] px-4 py-3">
                            <div className="text-caption text-ink-muted">{formatDate(new Date(selected.startDate))} {new Date(selected.startDate).getFullYear()}</div>
                            <div className="text-heading">{selected.name}</div>
                        </div>}
                        {shownDetail && shownDetail.points.length > 0 && <div className="glass absolute bottom-4 left-4 z-10 flex items-center gap-2 rounded-[10px] px-3 py-2 text-caption text-ink-muted">
                            <span>schnell</span>
                            <div className="h-1.5 w-28 rounded-full" style={{ background: `linear-gradient(90deg,${paceColor(0)},${paceColor(.5)},${paceColor(1)})` }} />
                            <span>langsam</span>
                        </div>}
                        {shownDetail && shownDetail.points.length === 0 && <div className="absolute inset-0 z-10 flex items-center justify-center bg-surface-sunken/80 text-ink-muted">
                            Kein GPS-Track{selected?.indoor ? " (Indoor-Lauf)" : ""}
                        </div>}
                        {loading && <div className="glass absolute right-4 top-4 z-10 rounded-full px-3 py-1 text-caption text-ink-muted">Lädt…</div>}
                    </RunMap>
                </Card>

                {selected && <Card className="lg:col-span-4">
                    <CardHead title="Details" right={selected.indoor ? "Indoor" : selected.sourceName ?? ""} />
                    <dl className="grid grid-cols-2 gap-x-3 gap-y-4 border-b border-line pb-4">
                        <Stat label="Distanz" value={km(selected.distance, 2)} unit="km" />
                        <Stat label="Zeit" value={formatDuration(selected.duration)} />
                        <Stat label="Ø Pace" value={formatPace(pace(selected))} unit="/km" />
                        <Stat label="Ø Puls" value={selected.averageHeartRate ? `${Math.round(selected.averageHeartRate)}` : "–"}
                            unit={selected.averageHeartRate ? `bpm${selected.maxHeartRate ? ` · max ${Math.round(selected.maxHeartRate)}` : ""}` : undefined} />
                        <Stat label="Kadenz" value={selected.cadence ? `${Math.round(selected.cadence)}` : "–"} unit={selected.cadence ? "spm" : undefined} />
                        <Stat label="Energie" value={selected.energy ? de(selected.energy) : "–"} unit={selected.energy ? "kcal" : undefined} />
                    </dl>
                    <CardHead title="Splits" right="Pace · Puls" small className="mb-1 mt-3.5" />
                    <Splits detail={shownDetail} onHover={setHighlightKm} />
                </Card>}

                <Card className="lg:col-span-8">
                    <CardHead title="Pace & Herzfrequenz" right={<>
                        <span className="inline-flex items-center gap-1.5"><span className="size-2 rounded-full bg-series-1" />Pace</span> &nbsp; <span className="inline-flex items-center gap-1.5"><span className="size-2 rounded-full bg-series-2" />Puls</span>
                    </>} />
                    <ProfileChart points={shownDetail?.points ?? []} onHover={setCursor} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Herzfrequenz-Zonen" right={`${inPeriod.length} ${inPeriod.length === 1 ? "Lauf" : "Läufe"}`} />
                    <Zones runs={inPeriod} />
                </Card>

                <Card className="lg:col-span-8">
                    <CardHead title="Distanz" right={period === "year" ? `pro Woche · Ziel ${WEEKLY_GOAL_KM} km` : "pro Tag"} />
                    <ActivityChart runs={inPeriod} period={period} from={current[0]} to={current[1]} onSelect={select} />
                </Card>

                <Card className="lg:col-span-4">
                    <CardHead title="Bestleistungen" right={`${new Date().getFullYear()}`} />
                    <Records runs={runs} onSelect={select} />
                </Card>

                <Card className="lg:col-span-12">
                    <CardHead title="Läufe" right={`${inPeriod.length} im Zeitraum`} />
                    <div className="flex flex-col">
                        {inPeriod.slice(0, shown).map((run) =>
                            <RunRow key={run.id} run={run} selected={run.id === selected?.id} onClick={() => {
                                select(run)
                                window.scrollTo({ top: 0, behavior: "smooth" })
                            }} />)}
                        {inPeriod.length > shown && <button onClick={() => setShown(shown + PAGE_SIZE)} className={cn(buttonVariants({ variant: "outline", size: "sm" }), "mt-3 self-start")}>
                            {Math.min(PAGE_SIZE, inPeriod.length - shown)} weitere anzeigen
                        </button>}
                        {!inPeriod.length && <p className="py-6 text-center text-ink-muted">Keine Läufe in diesem Zeitraum</p>}
                    </div>
                </Card>
            </div>
            <p className="text-caption text-ink">Daten aus Apple Health</p>
        </div>
    </div>
}

// MARK: KPIs

function Kpis({ runs, inPeriod, period, current }: { runs: RunSummary[], inPeriod: RunSummary[], period: Period, current: [Date, Date] }) {
    const cur = aggregate(inPeriod)
    const prev = period === "year" ? null : aggregate(runs.filter((r) => inRange(r, periodRange(period, 1))))
    const weeks = Math.max(1, (current[1].getTime() - current[0].getTime()) / 864e5 / 7)
    // the oldest run of the year, runs are sorted newest first
    const first = inPeriod[inPeriod.length - 1]
    const improvement = first ? Math.round(pace(first) - cur.pace) : 0

    const time = formatHours(cur.time).split(" ")
    const items: { label: string, icon: LucideIcon, value: React.ReactNode, delta: React.ReactNode }[] = [
        {
            label: "Distanz", icon: Route,
            value: <>{km(cur.distance)} <small>km</small></>,
            delta: prev ? <Delta current={cur.distance} previous={prev.distance} /> : <Note>Ø {km(cur.distance / weeks)} km / Woche</Note>,
        },
        {
            label: "Zeit", icon: Clock,
            value: <>{time.map((t, i) => i % 2 ? <small key={i}>{t} </small> : <span key={i}>{t} </span>)}</>,
            delta: prev ? <Delta current={cur.time} previous={prev.time} /> : <Note />,
        },
        {
            label: "Ø Pace", icon: Gauge,
            value: <>{cur.pace ? formatPace(cur.pace) : "–"} <small>/km</small></>,
            delta: prev
                ? <Delta current={cur.pace} previous={prev.pace} lowerIsBetter format={(c, p) => `${Math.round(Math.abs(c - p))} s`} />
                : first && improvement > 0
                    ? <Note className="font-semibold text-success">−{improvement} s seit {formatMonth(new Date(first.startDate))}</Note>
                    : <Note />,
        },
        {
            label: "Läufe", icon: Footprints,
            value: cur.count,
            delta: prev ? <Delta current={cur.count} previous={prev.count} /> : <Note>Ø {de(cur.count / weeks, 1)} / Woche</Note>,
        },
        {
            label: "Höhenmeter", icon: Mountain,
            value: <>{de(cur.elevation)} <small>m</small></>,
            delta: prev ? <Delta current={cur.elevation} previous={prev.elevation} /> : <Note />,
        },
    ]

    return <KpiGrid items={items} />
}

// MARK: cards

function Splits({ detail, onHover }: { detail: RunDetail | null, onHover: (km: number | null) => void }) {
    if (!detail) return <p className="py-4 text-ink-muted">Lädt…</p>
    const splits = detail.splits.map((s) => ({ ...s, pace: s.time / (s.distance / 1000) }))
    if (!splits.length) return <p className="py-4 text-ink-muted">Keine Splits ohne GPS-Track</p>
    const fast = Math.min(...splits.map((s) => s.pace)), slow = Math.max(...splits.map((s) => s.pace))
    const spread = Math.max(1, slow - fast)

    return <div className="mt-1 flex flex-col">
        {splits.map((s) => <div key={s.index} onMouseEnter={() => onHover(s.index)} onMouseLeave={() => onHover(null)}
            className="grid grid-cols-[28px_52px_minmax(0,1fr)_40px] items-center gap-2.5 rounded-[8px] px-1.5 py-1 hover:bg-surface-raised/50 dark:hover:bg-white/5">
            <div className="text-ink-muted">{s.distance < 950 ? de(s.index + s.distance / 1000, 1) : s.index + 1}</div>
            <div className="font-semibold tabular-nums">{formatPace(s.pace)}</div>
            <div><div className="h-2 rounded" style={{ width: `${45 + 55 * (1 - (s.pace - fast) / spread)}%`, background: paceColor((s.pace - fast) / spread) }} /></div>
            <div className="text-right text-ink-muted tabular-nums">{s.heartRate ? Math.round(s.heartRate) : "–"}</div>
        </div>)}
    </div>
}

function Zones({ runs }: { runs: RunSummary[] }) {
    const zones = ZONES.map((_, i) => runs.reduce((sum, r) => sum + (r.zoneSeconds[i] ?? 0), 0))
    const total = zones.reduce((a, b) => a + b, 0)
    if (!total) return <p className="py-6 text-ink-muted">Keine Herzfrequenzdaten im Zeitraum</p>
    const easy = (zones[0] + zones[1]) / total * 100

    return <>
        {ZONES.map((zone, i) => <div key={zone.key} className="grid grid-cols-[28px_minmax(0,1fr)_64px] items-center gap-2.5 py-1.5">
            <div className="font-semibold">{zone.key}</div>
            <div>
                <div className="mb-1 flex justify-between"><span>{zone.name}</span><span className="whitespace-nowrap text-caption text-ink-muted">{zone.bpm} bpm</span></div>
                <div className="h-1.5 overflow-hidden rounded-full bg-track">
                    <div className="h-full rounded-full" style={{ width: `${zones[i] / total * 100}%`, background: zone.color }} />
                </div>
            </div>
            <div className="text-right text-caption text-ink-muted">
                <b className="text-label font-semibold text-ink">{Math.round(zones[i] / total * 100)} %</b><br />{formatHours(zones[i])}
            </div>
        </div>)}
        <p className="mt-2.5 text-caption text-ink-muted">{Math.round(easy)} % locker (Z1–Z2). Für Grundlagenausdauer gelten ~80 % als guter Richtwert.</p>
    </>
}

function Records({ runs, onSelect }: { runs: RunSummary[], onSelect: (run: RunSummary) => void }) {
    const year = new Date().getFullYear()
    const ofYear = runs.filter((r) => new Date(r.startDate).getFullYear() === year)
    if (!ofYear.length) return <p className="py-6 text-ink-muted">Noch keine Läufe in {year}</p>

    const best = new Map<string, { run: RunSummary, time: number, distance: number }>()
    for (const run of ofYear)
        for (const effort of run.bestEfforts) {
            const current = best.get(effort.name)
            if (!current || effort.time < current.time) best.set(effort.name, { run, time: effort.time, distance: effort.distance })
        }
    const longest = ofYear.reduce((a, b) => b.distance > a.distance ? b : a)

    const rows = [
        ...[...best.entries()].sort((a, b) => a[1].distance - b[1].distance).map(([name, b]) => ({
            key: name, name, run: b.run,
            icon: name === "Halbmarathon" || name === "Marathon" ? Trophy : Zap,
            sub: `${formatDate(new Date(b.run.startDate))} · ${formatPace(b.time / (b.distance / 1000))} /km`,
            value: formatDuration(b.time),
        })),
        {
            key: "longest", name: "Längster Lauf", run: longest, icon: ArrowRight,
            sub: `${formatDate(new Date(longest.startDate))} · ${formatDuration(longest.duration)}`,
            value: `${km(longest.distance)} km`,
        },
    ]

    return <div className="flex flex-col">
        {rows.map((row, i) => <button key={row.key} onClick={() => onSelect(row.run)}
            className={cn("-mx-2 flex items-center gap-3 rounded-[8px] px-2 py-2 text-left hover:bg-surface-raised/50 dark:hover:bg-white/5", i > 0 && "border-t border-line")}>
            <IconTile icon={row.icon} size="sm" />
            <div className="min-w-0 flex-1">
                <div className="font-semibold">{row.name}</div>
                <div className="text-caption text-ink-muted">{row.sub}</div>
            </div>
            <div className="text-title font-bold tabular-nums">{row.value}</div>
        </button>)}
    </div>
}

function Thumbnail({ run, selected }: { run: RunSummary, selected: boolean }) {
    const pts = run.thumbnail
    if (pts.length < 2) return <div className="h-10 w-14 flex-none rounded-[10px] bg-brand-soft" />
    const lats = pts.map((p) => p[0]), lons = pts.map((p) => p[1])
    const a = Math.min(...lats), b = Math.max(...lats), c = Math.min(...lons), d = Math.max(...lons)
    // longitude degrees are shorter than latitude degrees away from the equator
    const k = Math.cos((a + b) / 2 * Math.PI / 180)
    const s = Math.min(48 / ((d - c) * k || 1e-9), 32 / (b - a || 1e-9))
    const points = pts.map(([lat, lon]) => `${(28 + (lon - (c + d) / 2) * k * s).toFixed(1)},${(20 - (lat - (a + b) / 2) * s).toFixed(1)}`).join(" ")
    return <svg viewBox="0 0 56 40" className={cn("h-10 w-12 flex-none rounded-[10px] transition-colors md:w-14", selected ? "bg-brand" : "bg-brand-soft")}>
        <polyline points={points} fill="none" stroke={selected ? "var(--on-brand)" : "var(--brand)"} strokeWidth={1.8} strokeLinejoin="round" strokeLinecap="round" />
    </svg>
}

function RunRow({ run, selected, onClick }: { run: RunSummary, selected: boolean, onClick: () => void }) {
    return <button onClick={onClick} className={cn(
        "grid w-full grid-cols-[48px_minmax(0,1fr)_auto] items-center gap-4 rounded-[12px] px-3 py-2 text-left md:grid-cols-[56px_minmax(0,1.4fr)_repeat(4,minmax(0,1fr))_16px]",
        selected ? "bg-brand-soft/60" : "hover:bg-surface-raised/50 dark:hover:bg-white/5")}>
        <Thumbnail run={run} selected={selected} />
        <div className="min-w-0">
            <div className="truncate font-bold">{run.name}</div>
            <div className="truncate text-caption text-ink-muted">{formatDate(new Date(run.startDate))} · {run.indoor ? "Indoor" : run.sourceName ?? "Outdoor"}</div>
        </div>
        <Cell value={km(run.distance, 2)} label="km" />
        <Cell value={formatDuration(run.duration)} label="Zeit" hide />
        <Cell value={formatPace(pace(run))} label="/km" hide />
        <Cell value={run.averageHeartRate ? `${Math.round(run.averageHeartRate)}` : "–"} label="bpm" hide />
        <ChevronRight className="hidden size-4 text-ink-muted md:block" />
    </button>
}

function Cell({ value, label, hide }: { value: string, label: string, hide?: boolean }) {
    return <div className={cn("font-semibold tabular-nums", hide && "hidden md:block")}>
        {value}<small className="block text-caption font-medium text-ink-muted">{label}</small>
    </div>
}

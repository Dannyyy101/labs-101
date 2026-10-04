'use client'

import { cn } from "@/lib/utils"
import { RunDetail, RunPoint, RunSummary } from "@/utils/types/run"
import { ArrowRight, ChevronRight, Trophy, Zap } from "lucide-react"
import { useMemo, useRef, useState, useTransition } from "react"
import { getRun } from "./action"
import { Card, CardHead, Delta, KpiGrid, Note, Period, PERIODS, periodRange, isInPeriod, Segmented, Stat } from "@/components/dashboard"
import { ActivityChart, ProfileChart, WEEKLY_GOAL_KM } from "./Charts"
import { de, formatDate, formatDuration, formatHours, formatMonth, formatPace, km, paceColor } from "./format"
import RunMap from "./RunMap"

const COLORS = { run: "#30d158", time: "#0a84ff", pace: "#ff9f0a", count: "#ff375f", elevation: "#bf5af2" }

const ZONES = [
    { key: "Z1", name: "Regeneration", bpm: "< 135", color: "#64d2ff" },
    { key: "Z2", name: "Grundlage", bpm: "135–147", color: "#30d158" },
    { key: "Z3", name: "Aerob", bpm: "148–159", color: "#ffd60a" },
    { key: "Z4", name: "Schwelle", bpm: "160–170", color: "#ff9f0a" },
    { key: "Z5", name: "Maximal", bpm: "> 170", color: "#ff453a" },
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
        return <div className="flex-1 w-full bg-muted/50 px-4 py-6 md:px-8">
            <div className="mx-auto max-w-7xl">
                <h1 className="text-4xl font-bold tracking-tight">Laufen</h1>
                <div className="mt-6 rounded-[18px] bg-card p-10 text-center shadow-sm">
                    <p className="text-lg font-semibold">Noch keine Läufe</p>
                    <p className="mt-1 text-muted-foreground">Die iOS-App überträgt deine Läufe aus Apple Health, sobald sie synchronisiert hat.</p>
                </div>
            </div>
        </div>
    }

    const shownDetail = detail && detail.summary.id === selected?.id ? detail : null

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-56 flex-1">
                    <h1 className="text-[34px] font-bold leading-tight tracking-tight">Laufen</h1>
                    <p className="whitespace-nowrap text-muted-foreground">
                        {period === "year"
                            ? `1. Jan – ${formatDate(current[1])} ${current[1].getFullYear()}`
                            : `${formatDate(current[0])} – ${formatDate(current[1])}`}
                    </p>
                </div>
                <Segmented value={period} options={PERIODS} onChange={changePeriod} />
            </div>

            <Kpis runs={runs} inPeriod={inPeriod} period={period} current={current} />

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                <Card className="p-0 lg:col-span-8 relative">
                    <RunMap points={shownDetail?.points ?? []} highlightKm={highlightKm} cursor={cursor}>
                        {selected && <div className="absolute left-4 top-4 z-10 max-w-[calc(100%-2rem)] rounded-[14px] bg-background/75 px-4 py-3 shadow-[0_4px_18px_rgba(0,0,0,.12)] backdrop-blur-xl">
                            <div className="text-sm font-medium text-muted-foreground">{formatDate(new Date(selected.startDate))} {new Date(selected.startDate).getFullYear()}</div>
                            <div className="text-xl font-bold tracking-tight">{selected.name}</div>
                        </div>}
                        {shownDetail && shownDetail.points.length > 0 && <div className="absolute bottom-4 left-4 z-10 flex items-center gap-2 rounded-xl bg-background/75 px-3 py-2 text-xs text-muted-foreground backdrop-blur-xl">
                            <span>schnell</span>
                            <div className="h-1.5 w-28 rounded-full" style={{ background: "linear-gradient(90deg,#30d158,#ffd60a,#ff9f0a,#ff453a)" }} />
                            <span>langsam</span>
                        </div>}
                        {shownDetail && shownDetail.points.length === 0 && <div className="absolute inset-0 z-10 flex items-center justify-center bg-muted/80 text-muted-foreground">
                            Kein GPS-Track{selected?.indoor ? " (Indoor-Lauf)" : ""}
                        </div>}
                        {loading && <div className="absolute right-4 top-4 z-10 rounded-full bg-background/75 px-3 py-1 text-xs text-muted-foreground backdrop-blur-xl">Lädt…</div>}
                    </RunMap>
                </Card>

                {selected && <Card className="lg:col-span-4">
                    <CardHead title="Details" right={selected.indoor ? "Indoor" : selected.sourceName ?? ""} />
                    <dl className="grid grid-cols-2 gap-x-3 gap-y-4 border-b pb-4">
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
                        <span className="text-[#0a84ff]">●</span> Pace &nbsp; <span className="text-[#ff375f]">●</span> Puls
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
                        {inPeriod.length > shown && <button onClick={() => setShown(shown + PAGE_SIZE)} className="mt-2 self-start px-3 py-1.5 font-medium text-[#0a84ff]">
                            {Math.min(PAGE_SIZE, inPeriod.length - shown)} weitere anzeigen
                        </button>}
                        {!inPeriod.length && <p className="py-6 text-center text-muted-foreground">Keine Läufe in diesem Zeitraum</p>}
                    </div>
                </Card>
            </div>
            <p className="text-xs text-muted-foreground">Daten aus Apple Health</p>
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
    const items: { label: string, color: string, value: React.ReactNode, delta: React.ReactNode }[] = [
        {
            label: "Distanz", color: COLORS.run,
            value: <>{km(cur.distance)} <small>km</small></>,
            delta: prev ? <Delta current={cur.distance} previous={prev.distance} /> : <Note>Ø {km(cur.distance / weeks)} km / Woche</Note>,
        },
        {
            label: "Zeit", color: COLORS.time,
            value: <>{time.map((t, i) => i % 2 ? <small key={i}>{t} </small> : <span key={i}>{t} </span>)}</>,
            delta: prev ? <Delta current={cur.time} previous={prev.time} /> : <Note />,
        },
        {
            label: "Ø Pace", color: COLORS.pace,
            value: <>{cur.pace ? formatPace(cur.pace) : "–"} <small>/km</small></>,
            delta: prev
                ? <Delta current={cur.pace} previous={prev.pace} lowerIsBetter format={(c, p) => `${Math.round(Math.abs(c - p))} s`} />
                : first && improvement > 0
                    ? <Note className="text-[#30d158]">▼ {improvement} s seit {formatMonth(new Date(first.startDate))}</Note>
                    : <Note />,
        },
        {
            label: "Läufe", color: COLORS.count,
            value: cur.count,
            delta: prev ? <Delta current={cur.count} previous={prev.count} /> : <Note>Ø {de(cur.count / weeks, 1)} / Woche</Note>,
        },
        {
            label: "Höhenmeter", color: COLORS.elevation,
            value: <>{de(cur.elevation)} <small>m</small></>,
            delta: prev ? <Delta current={cur.elevation} previous={prev.elevation} /> : <Note />,
        },
    ]

    return <KpiGrid items={items} />
}

// MARK: cards

function Splits({ detail, onHover }: { detail: RunDetail | null, onHover: (km: number | null) => void }) {
    if (!detail) return <p className="py-4 text-sm text-muted-foreground">Lädt…</p>
    const splits = detail.splits.map((s) => ({ ...s, pace: s.time / (s.distance / 1000) }))
    if (!splits.length) return <p className="py-4 text-sm text-muted-foreground">Keine Splits ohne GPS-Track</p>
    const fast = Math.min(...splits.map((s) => s.pace)), slow = Math.max(...splits.map((s) => s.pace))
    const spread = Math.max(1, slow - fast)

    return <div className="mt-1 flex flex-col">
        {splits.map((s) => <div key={s.index} onMouseEnter={() => onHover(s.index)} onMouseLeave={() => onHover(null)}
            className="grid grid-cols-[28px_52px_minmax(0,1fr)_40px] items-center gap-2.5 rounded-lg px-1.5 py-1 text-sm hover:bg-muted">
            <div className="text-muted-foreground">{s.distance < 950 ? de(s.index + s.distance / 1000, 1) : s.index + 1}</div>
            <div className="font-semibold tabular-nums">{formatPace(s.pace)}</div>
            <div><div className="h-2 rounded" style={{ width: `${45 + 55 * (1 - (s.pace - fast) / spread)}%`, background: paceColor((s.pace - fast) / spread) }} /></div>
            <div className="text-right text-muted-foreground tabular-nums">{s.heartRate ? Math.round(s.heartRate) : "–"}</div>
        </div>)}
    </div>
}

function Zones({ runs }: { runs: RunSummary[] }) {
    const zones = ZONES.map((_, i) => runs.reduce((sum, r) => sum + (r.zoneSeconds[i] ?? 0), 0))
    const total = zones.reduce((a, b) => a + b, 0)
    if (!total) return <p className="py-6 text-sm text-muted-foreground">Keine Herzfrequenzdaten im Zeitraum</p>
    const easy = (zones[0] + zones[1]) / total * 100

    return <>
        {ZONES.map((zone, i) => <div key={zone.key} className="grid grid-cols-[28px_minmax(0,1fr)_64px] items-center gap-2.5 py-1.5 text-sm">
            <div className="font-semibold">{zone.key}</div>
            <div>
                <div className="mb-1 flex justify-between"><span>{zone.name}</span><span className="whitespace-nowrap text-xs text-muted-foreground">{zone.bpm} bpm</span></div>
                <div className="h-2.5 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full" style={{ width: `${zones[i] / total * 100}%`, background: zone.color }} />
                </div>
            </div>
            <div className="text-right text-muted-foreground">
                <b className="font-semibold text-foreground">{Math.round(zones[i] / total * 100)} %</b><br />{formatHours(zones[i])}
            </div>
        </div>)}
        <p className="mt-2.5 text-xs text-muted-foreground">{Math.round(easy)} % locker (Z1–Z2). Für Grundlagenausdauer gelten ~80 % als guter Richtwert.</p>
    </>
}

const RECORD_COLORS: Record<string, string> = { "5 km": "#30d158", "10 km": "#0a84ff", "Halbmarathon": "#ff9f0a", "Marathon": "#ff375f" }

function Records({ runs, onSelect }: { runs: RunSummary[], onSelect: (run: RunSummary) => void }) {
    const year = new Date().getFullYear()
    const ofYear = runs.filter((r) => new Date(r.startDate).getFullYear() === year)
    if (!ofYear.length) return <p className="py-6 text-sm text-muted-foreground">Noch keine Läufe in {year}</p>

    const best = new Map<string, { run: RunSummary, time: number, distance: number }>()
    for (const run of ofYear)
        for (const effort of run.bestEfforts) {
            const current = best.get(effort.name)
            if (!current || effort.time < current.time) best.set(effort.name, { run, time: effort.time, distance: effort.distance })
        }
    const longest = ofYear.reduce((a, b) => b.distance > a.distance ? b : a)

    const rows = [
        ...[...best.entries()].sort((a, b) => a[1].distance - b[1].distance).map(([name, b]) => ({
            key: name, name, run: b.run, color: RECORD_COLORS[name] ?? "#0a84ff",
            icon: name === "Halbmarathon" || name === "Marathon" ? Trophy : Zap,
            sub: `${formatDate(new Date(b.run.startDate))} · ${formatPace(b.time / (b.distance / 1000))} /km`,
            value: formatDuration(b.time),
        })),
        {
            key: "longest", name: "Längster Lauf", run: longest, color: "#bf5af2", icon: ArrowRight,
            sub: `${formatDate(new Date(longest.startDate))} · ${formatDuration(longest.duration)}`,
            value: `${km(longest.distance)} km`,
        },
    ]

    return <div className="flex flex-col">
        {rows.map((row, i) => <button key={row.key} onClick={() => onSelect(row.run)}
            className={cn("flex items-center gap-3 py-2.5 text-left", i > 0 && "border-t")}>
            <div className="grid size-[34px] flex-none place-items-center rounded-[10px] text-white" style={{ background: row.color }}>
                <row.icon className="size-[18px]" strokeWidth={2.2} />
            </div>
            <div className="min-w-0 flex-1">
                <div className="font-semibold">{row.name}</div>
                <div className="text-xs text-muted-foreground">{row.sub}</div>
            </div>
            <div className="text-[17px] font-semibold tabular-nums">{row.value}</div>
        </button>)}
    </div>
}

function Thumbnail({ run, selected }: { run: RunSummary, selected: boolean }) {
    const pts = run.thumbnail
    if (pts.length < 2) return <div className="h-10 w-14 flex-none rounded-lg bg-muted" />
    const lats = pts.map((p) => p[0]), lons = pts.map((p) => p[1])
    const a = Math.min(...lats), b = Math.max(...lats), c = Math.min(...lons), d = Math.max(...lons)
    // longitude degrees are shorter than latitude degrees away from the equator
    const k = Math.cos((a + b) / 2 * Math.PI / 180)
    const s = Math.min(48 / ((d - c) * k || 1e-9), 32 / (b - a || 1e-9))
    const points = pts.map(([lat, lon]) => `${(28 + (lon - (c + d) / 2) * k * s).toFixed(1)},${(20 - (lat - (a + b) / 2) * s).toFixed(1)}`).join(" ")
    return <svg viewBox="0 0 56 40" className="h-10 w-12 flex-none rounded-lg bg-muted md:w-14">
        <polyline points={points} fill="none" stroke={selected ? "#0a84ff" : COLORS.run} strokeWidth={1.6} strokeLinejoin="round" />
    </svg>
}

function RunRow({ run, selected, onClick }: { run: RunSummary, selected: boolean, onClick: () => void }) {
    return <button onClick={onClick} className={cn(
        "grid w-full grid-cols-[48px_minmax(0,1fr)_auto] items-center gap-4 rounded-xl px-3 py-2.5 text-left md:grid-cols-[56px_minmax(0,1.4fr)_repeat(4,minmax(0,1fr))_16px]",
        selected ? "bg-[#0a84ff]/12" : "hover:bg-muted")}>
        <Thumbnail run={run} selected={selected} />
        <div className="min-w-0">
            <div className="truncate text-[15px] font-semibold">{run.name}</div>
            <div className="truncate text-xs text-muted-foreground">{formatDate(new Date(run.startDate))} · {run.indoor ? "Indoor" : run.sourceName ?? "Outdoor"}</div>
        </div>
        <Cell value={km(run.distance, 2)} label="km" />
        <Cell value={formatDuration(run.duration)} label="Zeit" hide />
        <Cell value={formatPace(pace(run))} label="/km" hide />
        <Cell value={run.averageHeartRate ? `${Math.round(run.averageHeartRate)}` : "–"} label="bpm" hide />
        <ChevronRight className="hidden size-3.5 text-muted-foreground/50 md:block" strokeWidth={2.6} />
    </button>
}

function Cell({ value, label, hide }: { value: string, label: string, hide?: boolean }) {
    return <div className={cn("text-[15px] tabular-nums", hide && "hidden md:block")}>
        {value}<small className="block text-[11px] text-muted-foreground">{label}</small>
    </div>
}

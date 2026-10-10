'use client'

import { Tooltip, useWidth } from "@/app/runs/Charts"
import { cn } from "@/lib/utils"
import { DayScore, Sleep, SleepStage } from "@/utils/types/overview"
import { useMemo, useState } from "react"
import { formatClock, formatSleep, shortDay } from "./format"

/** A score as a ring that fills up to 100. */
export function ScoreRing({ value, color, size = 132, stroke = 12, children }: { value: number | null, color: string, size?: number, stroke?: number, children?: React.ReactNode }) {
    const r = (size - stroke) / 2, c = 2 * Math.PI * r
    const filled = value == null ? 0 : Math.max(0.005, Math.min(1, value / 100))
    return <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
            <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeOpacity={.16} strokeWidth={stroke} />
            <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={stroke} strokeLinecap="round"
                strokeDasharray={`${filled * c} ${c}`} className="transition-[stroke-dasharray] duration-700" />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">{children}</div>
    </div>
}

export const STAGES: { stage: SleepStage, label: string, color: string }[] = [
    { stage: "AWAKE", label: "Wach", color: "#ff9f0a" },
    { stage: "REM", label: "REM", color: "#64d2ff" },
    { stage: "CORE", label: "Kern", color: "#0a84ff" },
    { stage: "DEEP", label: "Tief", color: "#5e5ce6" },
]

// sleep without a recorded stage is drawn as core sleep
const row = (stage: SleepStage) => stage === "ASLEEP" ? 2 : STAGES.findIndex((s) => s.stage === stage)

/** The stages of the night over time, awake at the top and deep sleep at the bottom like in Apple Health. */
export function Hypnogram({ sleep }: { sleep: Sleep }) {
    const [ref, W] = useWidth()
    const [hover, setHover] = useState<{ x: number, stage: Sleep["stages"][number] } | null>(null)
    const H = 168, ml = 44, mr = 8, mt = 4, mb = 22, rowH = (H - mt - mb) / STAGES.length

    const chart = useMemo(() => {
        if (W === 0) return null
        const start = new Date(sleep.start).getTime(), end = new Date(sleep.end).getTime()
        const X = (t: number) => ml + (t - start) / (end - start) * (W - ml - mr)
        const ticks: number[] = []
        const first = new Date(start)
        first.setMinutes(0, 0, 0)
        const step = (end - start) / 3600e3 > 6 ? 2 : 1
        for (let t = first.getTime() + 3600e3; t < end; t += 3600e3) if (new Date(t).getHours() % step === 0) ticks.push(t)
        const bars = sleep.stages.map((s) => {
            const x0 = X(Math.max(start, new Date(s.start).getTime())), x1 = X(Math.min(end, new Date(s.end).getTime()))
            return { s, x: x0, w: Math.max(1, x1 - x0), y: mt + row(s.stage) * rowH }
        })
        return { X, ticks, bars }
    }, [sleep, W, rowH])

    return <div ref={ref} className="relative" onMouseLeave={() => setHover(null)}>
        {chart && <svg width={W} height={H} className="block">
            {STAGES.map((s, i) => <g key={s.stage}>
                <rect x={ml} y={mt + i * rowH + 1} width={W - ml - mr} height={rowH - 2} rx={6} className="fill-muted" opacity={.6} />
                <text x={0} y={mt + i * rowH + rowH / 2 + 4} className="fill-muted-foreground text-[11px] font-medium">{s.label}</text>
            </g>)}
            {chart.bars.map(({ s, x, w, y }, i) => <rect key={i} x={x} y={y + 3} width={w} height={rowH - 6} rx={Math.min(4, w / 2)}
                fill={STAGES[row(s.stage)].color} onMouseEnter={() => setHover({ x: x + w / 2, stage: s })} />)}
            {chart.ticks.map((t) => <text key={t} x={chart.X(t)} y={H - 6} textAnchor="middle" className="fill-muted-foreground text-[11px] tabular-nums">
                {formatClock(new Date(t).toISOString())}
            </text>)}
        </svg>}
        {hover && <Tooltip x={Math.min(Math.max(hover.x - 60, 0), W - 130)} y={-8}>
            <div className="font-semibold">{STAGES[row(hover.stage.stage)].label}{hover.stage.stage === "ASLEEP" ? " (ohne Phase)" : ""}</div>
            <div className="text-muted-foreground">{formatClock(hover.stage.start)} – {formatClock(hover.stage.end)} · {formatSleep((new Date(hover.stage.end).getTime() - new Date(hover.stage.start).getTime()) / 1000)}</div>
        </Tooltip>}
    </div>
}

/** Heart rate of the whole day with the resting heart rate as a dashed line. */
export function HeartRateChart({ points, day, resting, color }: { points: [number, number][], day: string, resting: number | null, color: string }) {
    const [ref, W] = useWidth()
    const [hover, setHover] = useState<number | null>(null)
    const H = 200, ml = 34, mr = 8, mt = 10, mb = 24

    const chart = useMemo(() => {
        if (W === 0 || points.length < 2) return null
        const start = new Date(`${day}T00:00:00`).getTime(), end = start + 864e5
        const values = points.map((p) => p[1])
        const lo = Math.floor((Math.min(...values, resting ?? Infinity) - 8) / 10) * 10
        const hi = Math.ceil((Math.max(...values) + 5) / 10) * 10
        const X = (t: number) => ml + (t - start) / (end - start) * (W - ml - mr)
        const Y = (v: number) => mt + (hi - v) / (hi - lo) * (H - mt - mb)
        // more than 20 minutes without a reading leaves a gap
        let line = ""
        points.forEach(([t, v], i) => {
            line += `${i && t - points[i - 1][0] <= 20 * 60e3 ? "L" : "M"}${X(t).toFixed(1)},${Y(v).toFixed(1)}`
        })
        const step = hi - lo > 80 ? 40 : 20
        const yTicks: number[] = []
        for (let v = Math.ceil(lo / step) * step; v <= hi; v += step) yTicks.push(v)
        return { X, Y, line, yTicks, start }
    }, [points, day, resting, W])

    const move = (e: React.MouseEvent) => {
        if (!chart) return
        const x = e.clientX - e.currentTarget.getBoundingClientRect().left
        let best = 0
        points.forEach(([t], i) => { if (Math.abs(chart.X(t) - x) < Math.abs(chart.X(points[best][0]) - x)) best = i })
        setHover(best)
    }

    if (points.length < 2) return <div className="flex h-[200px] items-center justify-center text-muted-foreground">Keine Herzfrequenz an diesem Tag</div>

    const p = hover != null ? points[hover] : null
    return <div ref={ref} className="relative">
        {chart && <svg width={W} height={H} className="block" onMouseMove={move} onMouseLeave={() => setHover(null)}>
            <defs>
                <linearGradient id="hr-fill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0" stopColor={color} stopOpacity={.22} />
                    <stop offset="1" stopColor={color} stopOpacity={0} />
                </linearGradient>
            </defs>
            {chart.yTicks.map((v) => <g key={v}>
                <line x1={ml} x2={W - mr} y1={chart.Y(v)} y2={chart.Y(v)} className="stroke-border" />
                <text x={ml - 8} y={chart.Y(v) + 4} textAnchor="end" className="fill-muted-foreground text-[11px] tabular-nums">{v}</text>
            </g>)}
            {[0, 6, 12, 18, 24].map((h) => <text key={h} x={chart.X(chart.start + h * 3600e3)} y={H - 6}
                textAnchor={h === 0 ? "start" : h === 24 ? "end" : "middle"} className="fill-muted-foreground text-[11px] tabular-nums">
                {String(h % 24).padStart(2, "0")}:00
            </text>)}
            {resting != null && <g>
                <line x1={ml} x2={W - mr} y1={chart.Y(resting)} y2={chart.Y(resting)} stroke={color} strokeDasharray="4 4" strokeOpacity={.6} />
                <text x={W - mr - 4} y={chart.Y(resting) - 5} textAnchor="end" className="text-[10px] font-medium" fill={color}>Ruhepuls {Math.round(resting)}</text>
            </g>}
            <path d={chart.line} fill="none" stroke={color} strokeWidth={1.75} strokeLinejoin="round" />
            {p && <g>
                <line x1={chart.X(p[0])} x2={chart.X(p[0])} y1={mt} y2={H - mb} className="stroke-muted-foreground" strokeOpacity={.4} />
                <circle cx={chart.X(p[0])} cy={chart.Y(p[1])} r={4} fill={color} className="stroke-card" strokeWidth={2} />
            </g>}
        </svg>}
        {chart && p && <Tooltip x={Math.min(Math.max(chart.X(p[0]) - 50, 0), W - 110)} y={-6}>
            <div className="font-semibold tabular-nums">{Math.round(p[1])} bpm</div>
            <div className="text-muted-foreground tabular-nums">{formatClock(new Date(p[0]).toISOString())}</div>
        </Tooltip>}
    </div>
}

/** One bar per day of the history, the shown day is highlighted, a click opens the day. */
export function TrendBars({ history, day, value, color, format, max, onSelect }: {
    history: DayScore[], day: string, value: (d: DayScore) => number | null, color: string,
    format: (v: number) => string, max?: number, onSelect: (day: string) => void
}) {
    const [hover, setHover] = useState<number | null>(null)
    const values = history.map(value)
    const top = max ?? Math.max(1, ...values.map((v) => v ?? 0)) * 1.1
    const shown = values.filter((v): v is number => v != null)
    const average = shown.length ? shown.reduce((a, b) => a + b, 0) / shown.length : null

    return <div>
        <div className="mb-2 flex items-baseline justify-between text-sm">
            <span className="text-muted-foreground">Ø 14 Tage</span>
            <span className="font-semibold tabular-nums">{average != null ? format(average) : "–"}</span>
        </div>
        {/* one bar per day, too narrow for a full touch target on phones but neighbours only select the next day */}
        <div data-dense-targets className="relative flex h-24 items-end gap-[3px]" onMouseLeave={() => setHover(null)}>
            {history.map((d, i) => {
                const v = values[i]
                const selected = d.date === day
                return <button key={d.date} onClick={() => onSelect(d.date)} onMouseEnter={() => setHover(i)}
                    aria-label={`${shortDay(d.date)}: ${v != null ? format(v) : "keine Daten"}`}
                    className="group flex h-full flex-1 flex-col justify-end">
                    <div className={cn("w-full rounded-[4px] transition-opacity", !selected && "opacity-45 group-hover:opacity-75")}
                        style={{ height: v != null ? `${Math.max(3, v / top * 100)}%` : 3, background: v != null ? color : "var(--muted)" }} />
                </button>
            })}
            {hover != null && <div className="pointer-events-none absolute -top-11 z-10 whitespace-nowrap rounded-[10px] bg-card px-2.5 py-1.5 text-xs shadow-[0_6px_20px_rgba(0,0,0,.18)]"
                style={{ left: `${Math.min(hover / history.length * 100, 72)}%` }}>
                <div className="font-semibold tabular-nums">{values[hover] != null ? format(values[hover]!) : "Keine Daten"}</div>
                <div className="text-muted-foreground">{shortDay(history[hover].date)}</div>
            </div>}
        </div>
        <div className="mt-1.5 flex justify-between text-[11px] text-muted-foreground">
            <span>{shortDay(history[0].date)}</span>
            <span>{shortDay(history[history.length - 1].date)}</span>
        </div>
    </div>
}

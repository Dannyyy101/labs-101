'use client'

import { RunPoint, RunSummary } from "@/utils/types/run"
import { useEffect, useMemo, useRef, useState } from "react"
import { de, formatDate, formatDuration, formatPace, WEEKDAY } from "./format"

const PACE = "#0a84ff"
const PULSE = "#ff375f"
const RUN = "#30d158"
export const WEEKLY_GOAL_KM = 30

// width of the chart container, the SVGs are drawn in real pixels so the text never scales
function useWidth() {
    const ref = useRef<HTMLDivElement>(null)
    const [width, setWidth] = useState(0)
    useEffect(() => {
        if (!ref.current) return
        const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
        observer.observe(ref.current)
        return () => observer.disconnect()
    }, [])
    return [ref, width] as const
}

function Tooltip({ x, y, children }: { x: number, y: number, children: React.ReactNode }) {
    return <div className="pointer-events-none absolute z-10 whitespace-nowrap rounded-[10px] bg-card px-2.5 py-2 text-xs shadow-[0_6px_20px_rgba(0,0,0,.18)]" style={{ left: x, top: y }}>
        {children}
    </div>
}

/** Pace (filled, faster is higher) and heart rate over the distance of a run. */
export function ProfileChart({ points, onHover }: { points: RunPoint[], onHover: (point: RunPoint | null) => void }) {
    const [ref, W] = useWidth()
    const [hover, setHover] = useState<number | null>(null)
    const H = 240, ml = 44, mr = 40, mt = 10, mb = 26

    const chart = useMemo(() => {
        const withPace = points.filter((p) => p.pace != null)
        if (withPace.length < 2 || W === 0) return null
        const total = points[points.length - 1].distance
        // standing at the start or end would stretch the axis, so outliers are clipped to the edge
        const paces = withPace.map((p) => p.pace!).sort((a, b) => a - b)
        const pulses = points.map((p) => p.heartRate).filter((h): h is number => h != null)
        const pMin = paces[Math.floor(paces.length * .02)] - 10, pMax = paces[Math.ceil(paces.length * .98) - 1] + 10
        const hMin = pulses.length ? Math.min(...pulses) - 8 : 0, hMax = pulses.length ? Math.max(...pulses) + 4 : 1
        const X = (d: number) => ml + d / total * (W - ml - mr)
        const Yp = (v: number) => mt + (Math.min(pMax, Math.max(pMin, v)) - pMin) / (pMax - pMin) * (H - mt - mb)
        const Yh = (v: number) => mt + (hMax - v) / (hMax - hMin) * (H - mt - mb)
        const pacePath = withPace.map((p, i) => `${i ? "L" : "M"}${X(p.distance).toFixed(1)},${Yp(p.pace!).toFixed(1)}`).join("")
        const area = `${pacePath}L${X(withPace[withPace.length - 1].distance)},${H - mb}L${X(withPace[0].distance)},${H - mb}Z`
        let hr = ""
        points.forEach((p, i) => {
            if (p.heartRate == null) return
            const prev = points[i - 1]
            hr += `${prev?.heartRate != null ? "L" : "M"}${X(p.distance).toFixed(1)},${Yh(p.heartRate).toFixed(1)}`
        })
        const paceTicks: number[] = []
        for (let v = Math.ceil(pMin / 15) * 15; v <= pMax; v += 15) paceTicks.push(v)
        const hrTicks: number[] = []
        if (pulses.length) for (let v = Math.ceil(hMin / 10) * 10; v <= hMax; v += 10) hrTicks.push(v)
        const kmTicks: number[] = []
        const step = total > 14000 ? 2 : 1
        for (let k = 0; k * 1000 <= total; k += step) kmTicks.push(k)
        return { total, X, Yp, Yh, pacePath, area, hr, paceTicks, hrTicks, kmTicks }
    }, [points, W])

    const move = (e: React.MouseEvent<SVGRectElement>) => {
        if (!chart) return
        const rect = e.currentTarget.ownerSVGElement!.getBoundingClientRect()
        const d = Math.max(0, Math.min(chart.total, (e.clientX - rect.left - ml) / (W - ml - mr) * chart.total))
        let i = points.findIndex((p) => p.distance >= d)
        if (i < 0) i = points.length - 1
        setHover(i)
        onHover(points[i])
    }
    const leave = () => {
        setHover(null)
        onHover(null)
    }

    const p = hover != null ? points[hover] : null
    return <div ref={ref} className="relative">
        {!chart && W > 0 && <p className="py-20 text-center text-sm text-muted-foreground">Kein Pace-Verlauf für diesen Lauf</p>}
        {chart && <>
            <svg width={W} height={H} className="block overflow-visible">
                <defs>
                    <linearGradient id="pace-gradient" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0" stopColor={PACE} stopOpacity=".32" />
                        <stop offset="1" stopColor={PACE} stopOpacity="0" />
                    </linearGradient>
                </defs>
                {chart.paceTicks.map((v) => <g key={v}>
                    <line x1={ml} x2={W - mr} y1={chart.Yp(v)} y2={chart.Yp(v)} className="stroke-border" />
                    <text x={ml - 8} y={chart.Yp(v) + 4} textAnchor="end" fontSize={11} className="fill-muted-foreground">{formatPace(v)}</text>
                </g>)}
                {chart.hrTicks.map((v) => <text key={v} x={W - mr + 8} y={chart.Yh(v) + 4} fontSize={11} fill={PULSE} opacity={.8}>{v}</text>)}
                {chart.kmTicks.map((k) => <text key={k} x={chart.X(k * 1000)} y={H - 6} textAnchor="middle" fontSize={11} className="fill-muted-foreground">{k} km</text>)}
                <path d={chart.area} fill="url(#pace-gradient)" />
                <path d={chart.pacePath} fill="none" stroke={PACE} strokeWidth={2.2} strokeLinejoin="round" />
                <path d={chart.hr} fill="none" stroke={PULSE} strokeWidth={1.6} strokeLinejoin="round" opacity={.85} />
                {p && <>
                    <line x1={chart.X(p.distance)} x2={chart.X(p.distance)} y1={mt} y2={H - mb} className="stroke-foreground" opacity={.35} />
                    {p.pace != null && <circle cx={chart.X(p.distance)} cy={chart.Yp(p.pace)} r={4.5} fill={PACE} className="stroke-card" strokeWidth={2} />}
                    {p.heartRate != null && <circle cx={chart.X(p.distance)} cy={chart.Yh(p.heartRate)} r={4} fill={PULSE} className="stroke-card" strokeWidth={2} />}
                </>}
                <rect x={ml} y={0} width={Math.max(0, W - ml - mr)} height={H} fill="transparent" onMouseMove={move} onMouseLeave={leave} />
            </svg>
            {p && <Tooltip x={Math.min(W - 160, chart.X(p.distance) + 12)} y={8}>
                <div className="text-muted-foreground">{de(p.distance / 1000, 2)} km · {formatDuration(p.time)}</div>
                <div>
                    {p.pace != null && <><b className="text-sm" style={{ color: PACE }}>{formatPace(p.pace)}</b> /km &nbsp; </>}
                    {p.heartRate != null && <><b className="text-sm" style={{ color: PULSE }}>{Math.round(p.heartRate)}</b> bpm</>}
                </div>
            </Tooltip>}
        </>}
    </div>
}

export type Period = "7" | "30" | "year"

interface Bucket {
    label: string
    short: string
    distance: number
    count: number
    current: boolean
    run?: RunSummary
}

const startOfDay = (date: Date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())

function buckets(runs: RunSummary[], period: Period, from: Date, to: Date): Bucket[] {
    const result: Bucket[] = []
    const today = startOfDay(new Date())
    if (period === "year") {
        // weeks from the monday of the week with Jan 1
        const day = new Date(from)
        day.setDate(day.getDate() - (day.getDay() + 6) % 7)
        for (let week = 1; day <= to; week++) {
            const end = new Date(day)
            end.setDate(end.getDate() + 7)
            const list = runs.filter((r) => { const d = new Date(r.startDate); return d >= day && d < end })
            result.push({
                label: `KW ${week} · ab ${formatDate(day)}`,
                short: week % 4 === 1 ? `${day.getDate()}.${day.getMonth() + 1}.` : "",
                distance: list.reduce((sum, r) => sum + r.distance, 0) / 1000,
                count: list.length,
                current: today >= day && today < end,
            })
            day.setDate(day.getDate() + 7)
        }
        return result
    }
    for (const day = startOfDay(from); day <= to; day.setDate(day.getDate() + 1)) {
        const list = runs.filter((r) => startOfDay(new Date(r.startDate)).getTime() === day.getTime())
        result.push({
            label: formatDate(day),
            short: period === "7" ? WEEKDAY(day) : day.getDay() === 1 ? `${day.getDate()}.${day.getMonth() + 1}.` : "",
            distance: list.reduce((sum, r) => sum + r.distance, 0) / 1000,
            count: list.length,
            current: day.getTime() === today.getTime(),
            run: list[0],
        })
    }
    return result
}

/** Distance per day (7/30 days) or per week (year), a click on a day selects its run. */
export function ActivityChart({ runs, period, from, to, onSelect }: {
    runs: RunSummary[], period: Period, from: Date, to: Date, onSelect: (run: RunSummary) => void
}) {
    const [ref, W] = useWidth()
    const [hover, setHover] = useState<number | null>(null)
    const H = 220, ml = 34, mr = 8, mt = 18, mb = 26
    const data = useMemo(() => buckets(runs, period, from, to), [runs, period, from, to])

    const max = Math.max(period === "year" ? WEEKLY_GOAL_KM + 4 : 10, ...data.map((b) => b.distance)) * 1.08
    const bw = (W - ml - mr) / Math.max(1, data.length)
    const Y = (v: number) => mt + (1 - v / max) * (H - mt - mb)
    const ticks: number[] = []
    for (let v = 0; v <= max; v += max > 30 ? 10 : 5) ticks.push(v)

    const withRuns = data.filter((b) => b.distance > 0)
    const reference = period === "year"
        ? { value: WEEKLY_GOAL_KM, label: `Ziel ${WEEKLY_GOAL_KM} km` }
        : withRuns.length ? { value: withRuns.reduce((s, b) => s + b.distance, 0) / withRuns.length, label: "" } : null
    if (reference && period !== "year") reference.label = `Ø ${de(reference.value, 1)} km pro Lauf`

    const b = hover != null ? data[hover] : null
    return <div ref={ref} className="relative">
        {W > 0 && <svg width={W} height={H} className="block overflow-visible">
            {ticks.map((v) => <g key={v}>
                <line x1={ml} x2={W - mr} y1={Y(v)} y2={Y(v)} className="stroke-border" />
                <text x={ml - 6} y={Y(v) + 4} textAnchor="end" fontSize={11} className="fill-muted-foreground">{v}</text>
            </g>)}
            {data.map((bucket, i) => {
                const x = ml + i * bw + bw * .18, w = Math.max(2, bw * .64), r = Math.min(4, w / 2)
                return <g key={i}>
                    {bucket.distance > 0
                        ? <path d={`M${x},${Y(0)}V${Y(bucket.distance) + r}q0,-${r} ${r},-${r}h${w - 2 * r}q${r},0 ${r},${r}V${Y(0)}Z`}
                            fill={RUN} opacity={bucket.current || hover === i ? 1 : .55} />
                        : <rect x={x} y={Y(0) - 2} width={w} height={2} rx={1} className="fill-muted" />}
                    {bucket.short && <text x={x + w / 2} y={H - 6} textAnchor={period === "year" ? "start" : "middle"} fontSize={11} className="fill-muted-foreground">{bucket.short}</text>}
                    <rect x={ml + i * bw} y={0} width={bw} height={H - mb} fill="transparent"
                        style={{ cursor: bucket.run ? "pointer" : "default" }}
                        onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
                        onClick={() => bucket.run && onSelect(bucket.run)} />
                </g>
            })}
            {reference && <>
                <line x1={ml} x2={W - mr} y1={Y(reference.value)} y2={Y(reference.value)} className="stroke-foreground" strokeDasharray="4 4" opacity={.45} />
                <text x={W - mr} y={Y(reference.value) - 6} textAnchor="end" fontSize={11} className="fill-muted-foreground">{reference.label}</text>
            </>}
        </svg>}
        {b && <Tooltip x={Math.min(W - 150, Math.max(0, ml + hover! * bw - 40))} y={Math.max(0, Y(b.distance) - 56)}>
            <div className="text-muted-foreground">{b.label}</div>
            <b className="text-sm">{de(b.distance, 1)} km</b> · {b.count} {b.count === 1 ? "Lauf" : "Läufe"}
        </Tooltip>}
    </div>
}

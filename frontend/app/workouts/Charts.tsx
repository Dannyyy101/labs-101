'use client'

import { Period } from "@/components/dashboard"
import { WorkoutSession } from "@/utils/types/types"
import { useMemo, useState } from "react"
import { Tooltip, useWidth } from "../runs/Charts"
import { de, formatDate, WEEKDAY } from "../runs/format"
import { aggregate, COLORS, formatVolume } from "./stats"

interface Bucket {
    label: string
    short: string
    // tonnes
    volume: number
    count: number
    current: boolean
    session?: WorkoutSession
}

const startOfDay = (date: Date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())

function buckets(sessions: WorkoutSession[], period: Period, from: Date, to: Date): Bucket[] {
    const result: Bucket[] = []
    const today = startOfDay(new Date())
    const bucket = (list: WorkoutSession[]) => ({ volume: aggregate(list).volume / 1000, count: list.length })
    if (period === "year") {
        // weeks from the monday of the week with Jan 1
        const day = new Date(from)
        day.setDate(day.getDate() - (day.getDay() + 6) % 7)
        for (let week = 1; day <= to; week++) {
            const end = new Date(day)
            end.setDate(end.getDate() + 7)
            const list = sessions.filter((s) => { const d = new Date(s.startedAt); return d >= day && d < end })
            result.push({
                label: `KW ${week} · ab ${formatDate(day)}`,
                short: week % 4 === 1 ? `${day.getDate()}.${day.getMonth() + 1}.` : "",
                ...bucket(list),
                current: today >= day && today < end,
            })
            day.setDate(day.getDate() + 7)
        }
        return result
    }
    for (const day = startOfDay(from); day <= to; day.setDate(day.getDate() + 1)) {
        const list = sessions.filter((s) => startOfDay(new Date(s.startedAt)).getTime() === day.getTime())
        result.push({
            label: formatDate(day),
            short: period === "7" ? WEEKDAY(day) : day.getDay() === 1 ? `${day.getDate()}.${day.getMonth() + 1}.` : "",
            ...bucket(list),
            current: day.getTime() === today.getTime(),
            session: list[0],
        })
    }
    return result
}

/** Volume per day (7/30 days) or per week (year), a click on a day selects its workout. */
export function VolumeChart({ sessions, period, from, to, onSelect }: {
    sessions: WorkoutSession[], period: Period, from: Date, to: Date, onSelect: (session: WorkoutSession) => void
}) {
    const [ref, W] = useWidth()
    const [hover, setHover] = useState<number | null>(null)
    const H = 220, ml = 34, mr = 8, mt = 18, mb = 26
    const data = useMemo(() => buckets(sessions, period, from, to), [sessions, period, from, to])

    const max = Math.max(period === "year" ? 10 : 5, ...data.map((b) => b.volume)) * 1.08
    const bw = (W - ml - mr) / Math.max(1, data.length)
    const Y = (v: number) => mt + (1 - v / max) * (H - mt - mb)
    const step = [1, 2, 5, 10, 20, 50, 100].find((s) => max / s <= 5) ?? 200
    const ticks: number[] = []
    for (let v = 0; v <= max; v += step) ticks.push(v)

    const trained = data.filter((b) => b.volume > 0)
    const average = trained.length ? trained.reduce((s, b) => s + b.volume, 0) / trained.length : 0

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
                    {bucket.volume > 0
                        ? <path d={`M${x},${Y(0)}V${Y(bucket.volume) + r}q0,-${r} ${r},-${r}h${w - 2 * r}q${r},0 ${r},${r}V${Y(0)}Z`}
                            fill={COLORS.volume} opacity={bucket.current || hover === i ? 1 : .55} />
                        : bucket.count > 0
                            // a workout without weights (e.g. only bodyweight) still shows up
                            ? <rect x={x} y={Y(0) - 4} width={w} height={4} rx={2} fill={COLORS.volume} opacity={.55} />
                            : <rect x={x} y={Y(0) - 2} width={w} height={2} rx={1} className="fill-muted" />}
                    {bucket.short && <text x={x + w / 2} y={H - 6} textAnchor={period === "year" ? "start" : "middle"} fontSize={11} className="fill-muted-foreground">{bucket.short}</text>}
                    <rect x={ml + i * bw} y={0} width={bw} height={H - mb} fill="transparent"
                        style={{ cursor: bucket.session ? "pointer" : "default" }}
                        onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
                        onClick={() => bucket.session && onSelect(bucket.session)} />
                </g>
            })}
            {average > 0 && <>
                <line x1={ml} x2={W - mr} y1={Y(average)} y2={Y(average)} className="stroke-foreground" strokeDasharray="4 4" opacity={.45} />
                <text x={W - mr} y={Y(average) - 6} textAnchor="end" fontSize={11} className="fill-muted-foreground">
                    Ø {de(average, 1)} t pro {period === "year" ? "Woche" : "Training"}
                </text>
            </>}
        </svg>}
        {b && <Tooltip x={Math.min(W - 170, Math.max(0, ml + hover! * bw - 40))} y={Math.max(0, Y(b.volume) - 56)}>
            <div className="text-muted-foreground">{b.label}</div>
            <b className="text-sm">{formatVolume(b.volume * 1000).join(" ")}</b> · {b.count} {b.count === 1 ? "Training" : "Trainings"}
        </Tooltip>}
    </div>
}

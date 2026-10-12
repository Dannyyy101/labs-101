'use client'

import { Card, Segmented } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { addDays } from "@/app/foods/track/format"
import { de } from "@/app/runs/format"
import { ActivityDay } from "@/utils/types/profile"
import { useEffect, useMemo, useRef, useState } from "react"

export type Filter = "all" | "food" | "run" | "workout"

const FILTERS: { value: Filter, label: string }[] = [
    { value: "all", label: "Alles" },
    { value: "food", label: "Essen" },
    { value: "run", label: "Laufen" },
    { value: "workout", label: "Workouts" },
]

const COLORS: Record<Filter, string> = { all: "#30d158", food: "#ff9f0a", run: "#0a84ff", workout: "#ff375f" }
// opacity of the levels 1–4, level 0 is the empty cell
const LEVELS = [0.3, 0.5, 0.75, 1]
const WEEKS = 53
const MONTHS = ["Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]

const EMPTY = (date: string): ActivityDay => ({ date, meals: 0, runs: 0, runDistance: 0, workouts: 0, workoutMinutes: 0, loggedIn: false })

// 0–4, a day only in the app without anything tracked is the lightest green under "Alles"
function level(day: ActivityDay, filter: Filter): number {
    switch (filter) {
        case "food": return Math.min(4, day.meals)
        case "run": return day.runs === 0 ? 0 : day.runDistance < 5000 ? 1 : day.runDistance < 10000 ? 2 : day.runDistance < 15000 ? 3 : 4
        case "workout": return day.workouts === 0 ? 0 : day.workoutMinutes < 30 ? 1 : day.workoutMinutes < 60 ? 2 : day.workoutMinutes < 90 ? 3 : 4
        case "all": {
            const points = day.meals + 2 * day.runs + 2 * day.workouts
            return points === 0 ? (day.loggedIn ? 1 : 0) : points <= 2 ? 2 : points <= 4 ? 3 : 4
        }
    }
}

export const count = (day: ActivityDay, filter: Filter) =>
    filter === "food" ? day.meals : filter === "run" ? day.runs : filter === "workout" ? day.workouts : day.meals + day.runs + day.workouts

const weekday = (day: string) => (new Date(`${day}T12:00:00`).getDay() + 6) % 7 // monday 0
const longDay = (day: string) => new Date(`${day}T12:00:00`).toLocaleDateString("de-DE", { weekday: "long", day: "numeric", month: "long", year: "numeric" })

function cellColor(value: number, color: string) {
    return value === 0 ? undefined : `color-mix(in srgb, ${color} ${LEVELS[value - 1] * 100}%, transparent)`
}

/** GitHub style year of activity, Monday on top, the newest week on the right. */
export function ActivityHeatmap({ days, today }: { days: ActivityDay[], today: string }) {
    const [filter, setFilter] = useState<Filter>("all")
    const [selected, setSelected] = useState(today)
    const scroller = useRef<HTMLDivElement>(null)

    const byDate = useMemo(() => new Map(days.map((d) => [d.date, d])), [days])
    const first = useMemo(() => addDays(today, -weekday(today) - (WEEKS - 1) * 7), [today])
    const weeks = useMemo(() => Array.from({ length: WEEKS }, (_, w) =>
        Array.from({ length: 7 }, (_, d) => addDays(first, w * 7 + d))), [first])

    // phones only fit a part of the year, start with the newest weeks
    useEffect(() => {
        scroller.current?.scrollTo({ left: scroller.current.scrollWidth })
    }, [])

    const inRange = days.filter((d) => d.date >= first)
    const total = inRange.reduce((sum, d) => sum + count(d, filter), 0)
    const activeDays = inRange.filter((d) => level(d, filter) > 0).length
    const color = COLORS[filter]
    const day = byDate.get(selected) ?? EMPTY(selected)

    return <Card className="p-6">
        <div className="flex flex-wrap items-start gap-4">
            <div className="min-w-0">
                <h2 className="text-2xl font-bold tracking-tight tabular-nums">{de(total)} {total === 1 ? "Aktivität" : "Aktivitäten"}</h2>
                <p className="text-muted-foreground">Letzte 12 Monate · {de(activeDays)} aktive Tage</p>
            </div>
            <Segmented value={filter} options={FILTERS} onChange={setFilter} />
        </div>

        <div ref={scroller} className="-mx-1 mt-5 overflow-x-auto px-1 pb-1">
            <div className="grid min-w-[640px] grid-cols-[auto_1fr] gap-x-2">
                <div />
                <div className="grid text-xs text-muted-foreground" style={{ gridTemplateColumns: `repeat(${WEEKS}, minmax(0, 1fr))` }}>
                    {weeks.map((week, w) => {
                        // label the week the month starts in
                        const start = week.find((d) => d.endsWith("-01"))
                        return <span key={w} className="h-5 overflow-visible whitespace-nowrap">
                            {start && w < WEEKS - 1 ? MONTHS[Number(start.slice(5, 7)) - 1] : ""}
                        </span>
                    })}
                </div>

                <div className="grid grid-rows-7 gap-[3px] pr-1 text-xs text-muted-foreground">
                    {["Mo", "", "Mi", "", "Fr", "", ""].map((label, i) => <span key={i} className="flex items-center leading-none">{label}</span>)}
                </div>
                <div className="grid grid-flow-col grid-rows-7 gap-[3px]" style={{ gridTemplateColumns: `repeat(${WEEKS}, minmax(0, 1fr))` }}>
                    {weeks.flat().map((date) => {
                        if (date > today) return <span key={date} />
                        const d = byDate.get(date) ?? EMPTY(date)
                        const value = level(d, filter)
                        return <button key={date} type="button" onClick={() => setSelected(date)}
                            aria-label={`${longDay(date)}: ${count(d, filter)} Aktivitäten`}
                            aria-pressed={date === selected}
                            title={`${longDay(date)} · ${summary(d).map((c) => c.label).join(", ") || "Keine Aktivität"}`}
                            className={cn("aspect-square w-full rounded-[3px] bg-muted transition-transform hover:scale-125",
                                date === selected && "ring-2 ring-foreground/70 ring-offset-1 ring-offset-card")}
                            style={{ background: cellColor(value, color) }} />
                    })}
                </div>
            </div>
        </div>

        <div className="mt-5 flex flex-wrap items-end gap-4 border-t pt-5">
            <div className="min-w-0 flex-1">
                <h3 className="text-lg font-semibold">{longDay(selected)}</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                    {summary(day).map((chip) => <span key={chip.label} className="flex items-center gap-2 rounded-full bg-muted px-3.5 py-1.5 text-sm font-medium">
                        <span className="size-2 rounded-full" style={{ background: chip.color }} />{chip.label}
                    </span>)}
                    {summary(day).length === 0 && <span className="text-sm text-muted-foreground">Keine Aktivität an diesem Tag</span>}
                </div>
            </div>
            <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                Weniger
                {[0, 1, 2, 3, 4].map((v) => <span key={v} className="size-3 rounded-[3px] bg-muted" style={{ background: cellColor(v, color) }} />)}
                Mehr
            </div>
        </div>
    </Card>
}

function summary(day: ActivityDay): { label: string, color: string }[] {
    const chips = []
    if (day.meals) chips.push({ label: day.meals === 1 ? "1 Mahlzeit" : `${day.meals} Mahlzeiten`, color: COLORS.food })
    if (day.runs) chips.push({ label: `${day.runs === 1 ? "Lauf" : `${day.runs} Läufe`} ${de(day.runDistance / 1000, 1)} km`, color: COLORS.run })
    if (day.workouts) chips.push({ label: `${day.workouts === 1 ? "Workout" : `${day.workouts} Workouts`} ${de(day.workoutMinutes)} min`, color: COLORS.workout })
    if (day.loggedIn) chips.push({ label: "Eingeloggt", color: COLORS.all })
    return chips
}

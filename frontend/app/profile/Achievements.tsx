'use client'

import { Card, Segmented } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { Achievement } from "@/utils/types/profile"
import { Lock } from "lucide-react"
import Link from "next/link"
import { useMemo, useState } from "react"
import { AchievementView, CATEGORIES, Category, describe } from "./catalog"

type Filter = "all" | "done" | "open"

const FILTERS: { value: Filter, label: string }[] = [
    { value: "all", label: "Alle" },
    { value: "done", label: "Erreicht" },
    { value: "open", label: "Offen" },
]

// reached within this many days, the badge says "Neu"
const NEW_DAYS = 7

const formatDate = (day: string) => new Date(`${day}T12:00:00`).toLocaleDateString("de-DE", { day: "numeric", month: "short", year: "numeric" })
const daysBetween = (from: string, to: string) => Math.round((Date.parse(to) - Date.parse(from)) / 864e5)

export function useAchievements(achievements: Achievement[]) {
    return useMemo(() => achievements.map(describe).filter((a): a is AchievementView => a !== null), [achievements])
}

/** Round medal, colored once reached, grey with the progress as ring while open. */
export function Badge({ achievement, size = 76 }: { achievement: AchievementView, size?: number }) {
    const { category, icon: Icon, label, achievedAt, ratio } = achievement
    const style = CATEGORIES[category]
    const ring = 4
    const radius = size / 2 - ring / 2
    const circumference = 2 * Math.PI * radius

    return <div className="relative shrink-0" style={{ width: size, height: size }}>
        {achievedAt
            ? <div className="flex size-full flex-col items-center justify-center rounded-full text-white shadow-[inset_0_-4px_8px_rgba(0,0,0,.18),inset_0_3px_6px_rgba(255,255,255,.35),0_6px_14px_-4px_rgba(0,0,0,.3)]"
                style={{ background: style.gradient }}>
                <Icon className="drop-shadow-sm" style={{ width: size * 0.32, height: size * 0.32 }} strokeWidth={2.4} />
                {label && <span className="font-bold leading-none tabular-nums drop-shadow-sm" style={{ fontSize: size * 0.2, marginTop: size * 0.04 }}>{label}</span>}
            </div>
            : <>
                <div className="absolute inset-[6px] flex flex-col items-center justify-center rounded-full bg-muted text-muted-foreground/70">
                    <Icon style={{ width: size * 0.28, height: size * 0.28 }} strokeWidth={2.2} />
                    {label && <span className="font-bold leading-none tabular-nums" style={{ fontSize: size * 0.17, marginTop: size * 0.03 }}>{label}</span>}
                </div>
                <svg className="absolute inset-0 -rotate-90" viewBox={`0 0 ${size} ${size}`} aria-hidden>
                    <circle cx={size / 2} cy={size / 2} r={radius} fill="none" strokeWidth={ring} className="stroke-muted" />
                    {ratio > 0 && <circle cx={size / 2} cy={size / 2} r={radius} fill="none" strokeWidth={ring} strokeLinecap="round"
                        stroke={style.color} strokeDasharray={`${circumference * ratio} ${circumference}`} />}
                </svg>
                <span className="absolute -bottom-0.5 -right-0.5 flex size-6 items-center justify-center rounded-full border-2 border-card bg-muted-foreground/80 text-white">
                    <Lock className="size-3" strokeWidth={2.6} />
                </span>
            </>}
    </div>
}

export function ProgressBar({ ratio, color, className }: { ratio: number, color: string, className?: string }) {
    return <div className={cn("h-1.5 overflow-hidden rounded-full bg-muted", className)}>
        <div className="h-full rounded-full transition-[width] duration-500" style={{ width: `${Math.round(ratio * 100)}%`, background: color }} />
    </div>
}

/**
 * The three open achievements closest to being reached: seeing how little is
 * missing is what keeps people going (goal gradient), the full list alone
 * doesn't show it.
 */
export function NextUp({ achievements, calorieGoal }: { achievements: AchievementView[], calorieGoal: number | null }) {
    const next = achievements
        .filter((a) => !a.achievedAt && a.ratio > 0)
        .sort((a, b) => b.ratio - a.ratio)
        .slice(0, 3)

    if (!next.length) return null

    return <Card className="p-6">
        <div className="mb-4 flex items-baseline gap-2">
            <h2 className="text-xl font-bold tracking-tight">Fast geschafft</h2>
            <span className="text-sm text-muted-foreground">Deine nächsten Ziele</span>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
            {next.map((a) => {
                const color = CATEGORIES[a.category].color
                return <div key={a.id} className="flex items-center gap-3.5 rounded-2xl bg-muted/60 p-3.5">
                    <Badge achievement={a} size={56} />
                    <div className="min-w-0 flex-1">
                        <div className="flex items-baseline gap-2">
                            <p className="truncate font-semibold">{a.title}</p>
                            <span className="ml-auto text-sm font-semibold tabular-nums" style={{ color }}>{Math.floor(a.ratio * 100)} %</span>
                        </div>
                        <ProgressBar ratio={a.ratio} color={color} className="mt-1.5" />
                        <p className="mt-1.5 truncate text-xs text-muted-foreground">{a.progressText}</p>
                    </div>
                </div>
            })}
        </div>
        {calorieGoal == null && <p className="mt-3 text-sm text-muted-foreground">
            Für „Punktlandung“ fehlt noch ein Kalorienziel – <Link href="/settings" className="text-blue-500 hover:underline">in den Einstellungen festlegen</Link>.
        </p>}
    </Card>
}

/** All achievements by category, the tiers of a category in order, so the next step is always right after the last one. */
export function AchievementList({ achievements, today }: { achievements: AchievementView[], today: string }) {
    const [filter, setFilter] = useState<Filter>("all")
    const done = achievements.filter((a) => a.achievedAt).length

    const shown = achievements.filter((a) => filter === "all" || (filter === "done") === !!a.achievedAt)
    const groups = (Object.keys(CATEGORIES) as Category[])
        .map((category) => ({ category, items: shown.filter((a) => a.category === category) }))
        .filter((g) => g.items.length)

    return <Card className="p-6">
        <div className="flex flex-wrap items-start gap-4">
            <div>
                <h2 className="text-2xl font-bold tracking-tight">Achievements</h2>
                <p className="text-muted-foreground">{done} von {achievements.length} erreicht</p>
            </div>
            <Segmented value={filter} options={FILTERS} onChange={setFilter} />
        </div>
        <ProgressBar ratio={achievements.length ? done / achievements.length : 0} color="var(--foreground)" className="mt-5 h-2" />

        {groups.length === 0 && <p className="py-10 text-center text-muted-foreground">
            {filter === "done" ? "Noch nichts erreicht – das erste Achievement ist nicht weit." : "Alles erreicht. Respekt!"}
        </p>}

        {groups.map(({ category, items }) => {
            const style = CATEGORIES[category]
            const all = achievements.filter((a) => a.category === category)
            return <section key={category} className="mt-7">
                <div className="mb-3 flex items-center gap-2">
                    <span className="size-2.5 rounded-full" style={{ background: style.color }} />
                    <h3 className="font-semibold">{style.label}</h3>
                    <span className="text-sm text-muted-foreground tabular-nums">{all.filter((a) => a.achievedAt).length}/{all.length}</span>
                </div>
                <div className="grid grid-cols-2 gap-3 xl:grid-cols-4">
                    {items.map((a) => <AchievementCard key={a.id} achievement={a} today={today} />)}
                </div>
            </section>
        })}
    </Card>
}

function AchievementCard({ achievement: a, today }: { achievement: AchievementView, today: string }) {
    const color = CATEGORIES[a.category].color
    const isNew = a.achievedAt && daysBetween(a.achievedAt, today) < NEW_DAYS

    return <div className={cn("relative flex flex-col rounded-2xl bg-muted/60 p-4", !a.achievedAt && "bg-muted/35")}>
        {isNew && <span className="absolute right-3 top-3 rounded-full px-2 py-0.5 text-xs font-semibold text-white" style={{ background: color }}>Neu</span>}
        <Badge achievement={a} />
        <p className={cn("mt-3 font-semibold", !a.achievedAt && "text-foreground/80")}>{a.title}</p>
        <p className="text-sm text-muted-foreground">{a.description}</p>
        <div className="mt-auto pt-3">
            {a.achievedAt
                ? <p className="text-sm font-semibold" style={{ color }}>Erreicht am {formatDate(a.achievedAt)}</p>
                : <>
                    <div className="flex items-baseline justify-between gap-2 text-xs text-muted-foreground">
                        <span className="truncate">{a.progressText ?? "Noch nicht begonnen"}</span>
                        {a.ratio > 0 && <span className="shrink-0 font-semibold tabular-nums">{Math.floor(a.ratio * 100)} %</span>}
                    </div>
                    <ProgressBar ratio={a.ratio} color={color} className="mt-1.5" />
                </>}
        </div>
    </div>
}

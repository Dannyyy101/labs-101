'use client'

import { AvatarImage } from "@/components/AvatarImage"
import { Card, IconTile } from "@/components/dashboard"
import { buttonVariants } from "@/components/ui/button"
import { useUser } from "@/components/user-provider"
import { cn } from "@/lib/utils"
import { addDays } from "@/app/foods/track/format"
import { de } from "@/app/runs/format"
import { ActivityDay, Profile, Streak } from "@/utils/types/profile"
import { CircleCheck, Dumbbell, Flame, Footprints, Route, Utensils } from "lucide-react"
import { ReactNode, useMemo } from "react"
import { ActivityHeatmap } from "./ActivityHeatmap"
import { AchievementList, NextUp, ProgressBar, useAchievements } from "./Achievements"

const STREAK_GOALS = [3, 7, 14, 30, 50, 100, 200, 365]
// like the streak achievements
const STREAK_COLOR = "var(--series-4)"
const WEEKDAY_LETTERS = ["S", "M", "D", "M", "D", "F", "S"]

/** The own profile, or a friend's with `header` in place of the own name and picture. */
export default function ProfileView({ profile, header }: { profile: Profile, header?: ReactNode }) {
    const achievements = useAchievements(profile.achievements)

    return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto grid max-w-7xl grid-cols-1 gap-5 lg:grid-cols-[340px_minmax(0,1fr)]">
            <div className="flex flex-col gap-5">
                {header ?? <Header memberSince={profile.memberSince} />}
                <StreakCard streak={profile.streak} days={profile.days} today={profile.today} />
                <YearStats days={profile.days} today={profile.today} />
            </div>
            <div className="flex min-w-0 flex-col gap-5">
                <ActivityHeatmap days={profile.days} today={profile.today} />
                <NextUp achievements={achievements} calorieGoal={profile.calorieGoal} own={!header} />
                <AchievementList achievements={achievements} today={profile.today} />
            </div>
        </div>
    </div>
}

function Header({ memberSince }: { memberSince: string | null }) {
    const user = useUser()
    const initial = user?.name?.trim().charAt(0).toUpperCase() ?? ""
    const since = memberSince && new Date(memberSince).toLocaleDateString("de-DE", { month: "short", year: "numeric" })

    return <div className="flex flex-row items-center gap-5 lg:flex-col lg:items-start lg:px-1">
        <div className="flex size-24 shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand text-on-brand text-5xl font-semibold lg:size-44 lg:text-7xl">
            <AvatarImage src={user?.image ?? null} alt="" size={176} className="size-full object-cover" fallback={initial} />
        </div>
        <div className="min-w-0 flex-1 lg:w-full">
            <h1 className="truncate text-[22px] font-bold leading-7 tracking-tight lg:text-[34px]">{user?.name}</h1>
            <p className="truncate text-ink-muted">{user?.email}</p>
            {since && <p className="text-body text-ink-muted">Mitglied seit {since}</p>}
            <a href="/auth/profile" target="_blank" className={cn(buttonVariants({ variant: "outline" }), "mt-3 w-full lg:mt-5")}>
                Profil bearbeiten
            </a>
        </div>
    </div>
}

function StreakCard({ streak, days, today }: { streak: Streak, days: ActivityDay[], today: string }) {
    const loggedIn = useMemo(() => new Set(days.filter((d) => d.loggedIn).map((d) => d.date)), [days])
    // two weeks, the oldest first
    const last = Array.from({ length: 14 }, (_, i) => addDays(today, i - 13))
    const goal = STREAK_GOALS.find((g) => g > streak.current) ?? Math.ceil((streak.current + 1) / 100) * 100

    return <Card className="p-5">
        <div className="flex items-center gap-2">
            <h2 className="text-heading">Login-Streak</h2>
            {streak.today
                ? <span className="ml-auto flex items-center gap-1 text-body font-semibold text-success"><CircleCheck className="size-4" />Heute eingeloggt</span>
                : <span className="ml-auto text-body font-medium text-ink-muted">Heute noch offen</span>}
        </div>

        <div className="mt-4 flex items-center gap-3.5">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-[12px]" style={{ background: `color-mix(in srgb, ${STREAK_COLOR} 16%, transparent)`, color: STREAK_COLOR }}>
                <Flame className="size-7" fill="currentColor" />
            </div>
            <div className="min-w-0">
                <div className="text-[40px] font-bold leading-none tracking-tight tabular-nums">{streak.current}</div>
                <div className="mt-1 whitespace-nowrap text-ink-muted">{streak.current === 1 ? "Tag" : "Tage"} in Folge</div>
            </div>
            <div className="ml-auto text-right">
                <div className="text-body text-ink-muted">Rekord</div>
                <div className="whitespace-nowrap text-[20px] font-bold tabular-nums">{streak.record} {streak.record === 1 ? "Tag" : "Tage"}</div>
            </div>
        </div>

        <div className="mt-5 grid grid-cols-7 gap-x-2 gap-y-1.5">
            {last.map((day) => {
                const done = loggedIn.has(day)
                const date = new Date(`${day}T12:00:00`)
                return <div key={day} className="flex flex-col items-center gap-1">
                    <div className={cn("flex aspect-square w-full max-w-11 items-center justify-center rounded-[12px]",
                        done ? "text-white" : "glass-inset text-ink-muted/40",
                        day === today && "ring-2 ring-brand ring-offset-2 ring-offset-transparent")}
                        style={done ? { background: STREAK_COLOR } : undefined}
                        title={date.toLocaleDateString("de-DE", { weekday: "long", day: "numeric", month: "long" })}>
                        <Flame className="size-4" fill={done ? "currentColor" : "none"} />
                    </div>
                    <span className="text-label text-ink-muted">{WEEKDAY_LETTERS[date.getDay()]}</span>
                </div>
            })}
        </div>

        <div className="mt-5 border-t border-line pt-4">
            <div className="flex items-baseline justify-between text-body">
                <span className="font-semibold">Nächstes Ziel: {goal} Tage</span>
                <span className="text-ink-muted">noch {goal - streak.current} {goal - streak.current === 1 ? "Tag" : "Tage"}</span>
            </div>
            <ProgressBar ratio={streak.current / goal} color={STREAK_COLOR} className="mt-2 h-2" />
        </div>
    </Card>
}

function YearStats({ days, today }: { days: ActivityDay[], today: string }) {
    const from = addDays(today, -364)
    const year = days.filter((d) => d.date >= from)
    const sum = (f: (d: ActivityDay) => number) => year.reduce((s, d) => s + f(d), 0)

    const rows = [
        { label: "Läufe", value: de(sum((d) => d.runs)), icon: Footprints },
        { label: "Kilometer", value: de(sum((d) => d.runDistance) / 1000), icon: Route },
        { label: "Mahlzeiten", value: de(sum((d) => d.meals)), icon: Utensils },
        { label: "Workouts", value: de(sum((d) => d.workouts)), icon: Dumbbell },
    ]

    return <Card className="p-0">
        <h2 className="px-5 pb-1 pt-4 text-heading">Letzte 12 Monate</h2>
        <ul>
            {rows.map((row) => <li key={row.label} className="flex items-center gap-3 pl-5">
                <IconTile icon={row.icon} size="sm" />
                <div className="flex flex-1 items-center justify-between border-b border-line py-3 pr-5 [li:last-child_&]:border-b-0">
                    <span className="font-semibold">{row.label}</span>
                    <span className="text-heading tabular-nums">{row.value}</span>
                </div>
            </li>)}
        </ul>
    </Card>
}

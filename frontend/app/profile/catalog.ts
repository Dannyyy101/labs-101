import { Achievement } from "@/utils/types/profile"
import { Crown, Dumbbell, Flame, Footprints, LucideIcon, Medal, Mountain, Route, Target, Timer, Utensils } from "lucide-react"
import { de, formatPace } from "@/app/runs/format"

// texts and looks of the achievements, the backend only knows ids, progress and the day they were reached

export type Category = "food" | "run" | "workout" | "streak"

export const CATEGORIES: Record<Category, { label: string, color: string, gradient: string }> = {
    food: { label: "Ernährung", color: "#ff9f0a", gradient: "linear-gradient(145deg,#ffd60a,#ff9f0a 55%,#f07c00)" },
    run: { label: "Laufen", color: "#0a84ff", gradient: "linear-gradient(145deg,#64d2ff,#0a84ff 55%,#0060df)" },
    workout: { label: "Training", color: "#ff375f", gradient: "linear-gradient(145deg,#ff8aa1,#ff375f 55%,#e0153f)" },
    streak: { label: "Streak", color: "#bf5af2", gradient: "linear-gradient(145deg,#da8fff,#bf5af2 55%,#9a3bd1)" },
}

interface Definition {
    title: string
    description: string
    category: Category
    icon: LucideIcon
    // number on the badge
    label?: string
    // "312 / 500 Mahlzeiten"
    format: (progress: number, target: number) => string
    lowerIsBetter?: boolean
}

const count = (unit: string) => (progress: number, target: number) => `${de(Math.min(progress, target))} / ${de(target)} ${unit}`
const kilometers = (progress: number, target: number) => `${de(Math.min(progress, target))} / ${de(target)} km`
const longest = (progress: number, target: number) => `Längster Lauf ${de(progress / 1000, 1)} / ${de(target / 1000, 1)} km`
const days = (progress: number, target: number) => `Beste Serie ${de(Math.min(progress, target))} / ${de(target)} Tage`

const DEFINITIONS: Record<string, Definition> = {
    MEALS_10: { title: "Guten Appetit", description: "10 Mahlzeiten eingetragen", category: "food", icon: Utensils, label: "10", format: count("Mahlzeiten") },
    MEALS_100: { title: "100 Mahlzeiten", description: "100 Mahlzeiten eingetragen", category: "food", icon: Utensils, label: "100", format: count("Mahlzeiten") },
    MEALS_500: { title: "500 Mahlzeiten", description: "500 Mahlzeiten eingetragen", category: "food", icon: Utensils, label: "500", format: count("Mahlzeiten") },
    MEALS_1000: { title: "Food-Tracker", description: "1.000 Mahlzeiten eingetragen", category: "food", icon: Utensils, label: "1k", format: count("Mahlzeiten") },
    CALORIE_GOAL_7: { title: "Punktlandung", description: "7 Tage in Folge das Kalorienziel (± 10 %) getroffen", category: "food", icon: Target, label: "7", format: days },

    FIRST_RUN: { title: "Erster Lauf", description: "Den ersten Lauf aufgezeichnet", category: "run", icon: Footprints, format: count("Lauf") },
    RUN_10K: { title: "10 km", description: "Ein Lauf über 10 km", category: "run", icon: Route, label: "10", format: longest },
    HALF_MARATHON: { title: "Halbmarathon", description: "Ein Lauf über 21,1 km", category: "run", icon: Medal, format: longest },
    MARATHON: { title: "Marathon", description: "Ein Lauf über 42,2 km", category: "run", icon: Crown, format: longest },
    SUB_25: {
        title: "Sub 25", description: "5 km unter 25 Minuten", category: "run", icon: Timer, lowerIsBetter: true,
        format: (progress) => `Bestzeit ${formatPace(progress)} min`,
    },
    KM_100: { title: "100 km", description: "Insgesamt 100 km gelaufen", category: "run", icon: Route, label: "100", format: kilometers },
    KM_500: { title: "500 km", description: "Insgesamt 500 km gelaufen", category: "run", icon: Route, label: "500", format: kilometers },
    KM_1000: { title: "1.000 km", description: "Insgesamt 1.000 km gelaufen", category: "run", icon: Mountain, label: "1k", format: kilometers },

    WORKOUTS_1: { title: "Erstes Workout", description: "Das erste Krafttraining abgeschlossen", category: "workout", icon: Dumbbell, format: count("Workout") },
    WORKOUTS_10: { title: "10 Workouts", description: "10 Krafttrainings abgeschlossen", category: "workout", icon: Dumbbell, label: "10", format: count("Workouts") },
    WORKOUTS_50: { title: "50 Workouts", description: "50 Krafttrainings abgeschlossen", category: "workout", icon: Dumbbell, label: "50", format: count("Workouts") },
    WORKOUTS_100: { title: "Eisen-Club", description: "100 Krafttrainings abgeschlossen", category: "workout", icon: Dumbbell, label: "100", format: count("Workouts") },

    STREAK_7: { title: "Eine Woche", description: "7 Tage in Folge eingeloggt", category: "streak", icon: Flame, label: "7", format: days },
    STREAK_30: { title: "Ein Monat", description: "30 Tage in Folge eingeloggt", category: "streak", icon: Flame, label: "30", format: days },
    STREAK_100: { title: "Unaufhaltsam", description: "100 Tage in Folge eingeloggt", category: "streak", icon: Flame, label: "100", format: days },
    STREAK_365: { title: "Ein ganzes Jahr", description: "365 Tage in Folge eingeloggt", category: "streak", icon: Flame, label: "365", format: days },
}

export interface AchievementView extends Achievement, Definition {
    // 0–1
    ratio: number
    // the progress line, null while there is nothing to show
    progressText: string | null
}

export function describe(achievement: Achievement): AchievementView | null {
    const definition = DEFINITIONS[achievement.id]
    if (!definition) return null
    const { progress, target } = achievement
    const ratio = achievement.achievedAt ? 1
        : progress == null ? 0
            // the time to beat is the target, every second faster than twice of it counts
            : definition.lowerIsBetter ? Math.max(0, Math.min(1, (2 * target - progress) / target))
                : Math.min(1, progress / target)
    return {
        ...achievement,
        ...definition,
        ratio,
        progressText: progress == null || (progress === 0 && !definition.lowerIsBetter) ? null : definition.format(progress, target),
    }
}

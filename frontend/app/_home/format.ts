import { de } from "@/app/runs/format"

const WEEKDAYS_SHORT = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]

// "07:30"
export const formatClock = (iso: string) =>
    new Date(iso).toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })

// seconds -> "7 h 32 min"
export const formatSleep = (seconds: number) => {
    const total = Math.round(seconds / 60)
    const h = Math.floor(total / 60), m = total % 60
    return h ? `${h} h ${String(m).padStart(2, "0")} min` : `${m} min`
}

// "2026-10-04" -> "Sa 4."
export const shortDay = (day: string) => {
    const date = new Date(`${day}T12:00:00`)
    return `${WEEKDAYS_SHORT[date.getDay()]} ${date.getDate()}.`
}

export const percent = (value: number, decimals = 0) => `${de(value * 100, decimals)} %`

// HKWorkoutActivityType
const WORKOUTS: Record<number, string> = {
    13: "Radfahren", 16: "Crosstrainer", 20: "Funktionelles Krafttraining", 24: "Wandern", 35: "Rudern",
    37: "Laufen", 44: "Treppensteigen", 46: "Schwimmen", 50: "Krafttraining", 52: "Gehen", 57: "Yoga",
    59: "Core-Training", 62: "Beweglichkeit", 63: "HIIT", 73: "Cardio", 80: "Cool-down", 84: "Tauchen",
}

export const workoutName = (activityType: number) => WORKOUTS[activityType] ?? "Training"

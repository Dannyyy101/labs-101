import { de } from "@/app/runs/format"
import { Exercise } from "@/utils/types/workoutTypes"
import { SessionSet, WorkoutSession } from "@/utils/types/types"

export const COLORS = { count: "#ff375f", time: "#0a84ff", volume: "#ff9f0a", sets: "#30d158", reps: "#bf5af2" }

/** Seconds from the start to the end (or now, while it is running). */
export const sessionDuration = (session: WorkoutSession, now = Date.now()) =>
    Math.max(0, ((session.endedAt ? new Date(session.endedAt).getTime() : now) - new Date(session.startedAt).getTime()) / 1000)

export const doneSets = (session: WorkoutSession) => session.exercises.flatMap((e) => e.sets.filter((s) => s.done))

/** Weight times reps of the sets, in kg. */
export const volume = (sets: { reps: number, weightKg: number }[]) => sets.reduce((sum, s) => sum + s.reps * s.weightKg, 0)

/** Estimated one rep max (Epley), only meaningful up to ~12 reps. */
export const oneRepMax = (set: { reps: number, weightKg: number }) => set.reps <= 1 ? set.weightKg : set.weightKg * (1 + set.reps / 30)

/** [value, unit] – tonnes from 10 t on, so the KPI stays short. */
export const formatVolume = (kg: number): [string, string] =>
    kg >= 10000 ? [de(kg / 1000, 1), "t"] : [de(kg), "kg"]

export const formatWeight = (kg: number) => de(kg, Number.isInteger(kg) ? 0 : 1)

export const formatSet = (set: { reps: number, weightKg: number }) =>
    set.weightKg ? `${set.reps} × ${formatWeight(set.weightKg)} kg` : `${set.reps} Wdh`

export function aggregate(sessions: WorkoutSession[]) {
    const sets = sessions.flatMap(doneSets)
    return {
        count: sessions.length,
        time: sessions.reduce((sum, s) => sum + sessionDuration(s), 0),
        volume: volume(sets),
        sets: sets.length,
        reps: sets.reduce((sum, s) => sum + s.reps, 0),
    }
}

/** The sets of the exercise in the newest of the sessions (sorted newest first) that contains it. */
export function lastSets(sessions: WorkoutSession[], exerciseId: number | null): SessionSet[] | null {
    if (exerciseId == null) return null
    for (const session of sessions) {
        const found = session.exercises.find((e) => e.exerciseId === exerciseId && e.sets.some((s) => s.done))
        if (found) return found.sets.filter((s) => s.done)
    }
    return null
}

export const MUSCLES: Record<string, string> = {
    "chest": "Brust", "biceps": "Bizeps", "triceps": "Trizeps", "deltoids": "Schultern", "abs": "Bauch",
    "obliques": "Seitlicher Bauch", "quadriceps": "Quadrizeps", "hamstring": "Beinbeuger", "gluteal": "Gesäß",
    "calves": "Waden", "upper-back": "Oberer Rücken", "lower-back": "Unterer Rücken", "trapezius": "Trapez",
    "forearm": "Unterarme", "adductors": "Adduktoren", "tibialis": "Schienbein", "neck": "Nacken",
    "knees": "Knie", "ankles": "Knöchel", "hands": "Hände", "feet": "Füße", "head": "Kopf", "hair": "Kopf",
}

/** Number of sets per muscle, most trained first. */
export function setsPerMuscle(items: { exerciseId: number | null, sets: number }[], exercises: Map<number, Exercise>) {
    const result = new Map<string, number>()
    for (const item of items) {
        const exercise = item.exerciseId != null ? exercises.get(item.exerciseId) : undefined
        // a muscle listed for both sides only counts once
        const slugs = new Set(exercise?.bodyParts.map((p) => p.slug).filter((s): s is NonNullable<typeof s> => !!s))
        slugs.forEach((slug) => result.set(slug, (result.get(slug) ?? 0) + item.sets))
    }
    return [...result.entries()].sort((a, b) => b[1] - a[1])
}

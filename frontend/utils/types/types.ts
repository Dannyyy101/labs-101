import type { Schemas } from "@/lib/api/types"

export enum WorkoutType {
    CYCLING = 13,
    RUNNING = 37,
    SWIMMING = 46,
    STRENGTH_TRAINING = 50,
    WALKING = 52
}

export interface Result<T> {
    value: T | null,
    error: Error | null
}

// see ExerciseItem in the backend, fails to compile when it gets another type
export type WorkoutExerciseType = WorkoutExercise["type"]
export const WorkoutExerciseType = {
    STRENGTH_EXERCISE: "STRENGTH_EXERCISE",
    CARDIO_EXERCISE: "CARDIO_EXERCISE",
} as const satisfies { [T in WorkoutExerciseType]: T }

export type Workout = Schemas["WorkoutDto"]
export type WorkoutUser = Schemas["CalendarUserDto"]

export type WorkoutExercise = StrengthExercise | CardioExercise
export type StrengthExercise = Schemas["StrengthItemDto"]
// cardio like running or rowing, only with its minutes, sets are always empty
export type CardioExercise = Schemas["CardioItemDto"]
// restSeconds is the rest after the set, null for the default
export type ExerciseSet = Schemas["SetDto"]

// a workout of the user, see WorkoutSessionController in the backend
export type WorkoutSession = Schemas["WorkoutSessionDto"]
export type SessionExercise = Schemas["SessionExerciseDto"]
// only done cardio counts, like only done sets
export type SessionCardio = Schemas["SessionCardioDto"]
export type SessionSet = Schemas["SessionSetDto"]

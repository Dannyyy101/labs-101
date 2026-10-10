import { Exercise } from "./workoutTypes"

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

export enum WorkoutExerciseType {
    STRENGTH_EXERCISE = "STRENGTH_EXERCISE",
    RUN_EXERCISE = "RUN_EXERCISE",
    SWIMMING_EXERCISE = "SWIMMING_EXERCISE"
}

export interface Workout {
    id: number
    name: string
    workoutExercises: WorkoutExercise[]
}

export type WorkoutExercise = StrengthExercise

export interface BaseWorkoutExercise {
    type: WorkoutExerciseType
    exercise: Exercise
}

export interface StrengthExercise extends BaseWorkoutExercise {
    type: WorkoutExerciseType.STRENGTH_EXERCISE
    sets: ExerciseSet[]
}

export interface ExerciseSet {
    order: number,
    reps: number
    weightKg: number
    rpe: number | null
    // the rest after the set, null for the default
    restSeconds: number | null
}

// a workout of the user, see WorkoutSessionController in the backend
export interface WorkoutSession {
    id: number
    // the template it was started from, null for a free workout
    workoutId: number | null
    name: string
    startedAt: string
    // null while the workout is running
    endedAt: string | null
    exercises: SessionExercise[]
}

export interface SessionExercise {
    exerciseId: number | null
    // kept so the history still reads right after the exercise is renamed or deleted
    name: string
    sets: SessionSet[]
}

export interface SessionSet {
    reps: number
    weightKg: number
    rpe: number | null
    done: boolean
    // the rest after the set, null for the default
    restSeconds: number | null
}
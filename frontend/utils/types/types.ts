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
    SWIMMING_EXERCISE = "SWIMMING_EXERCISE",
    CARDIO_EXERCISE = "CARDIO_EXERCISE"
}

export interface Workout {
    id: number
    name: string
    workoutExercises: WorkoutExercise[]
    // name of the creator, null if unknown
    ownerName: string | null
    // only the creator can change or delete the template
    editable: boolean
    // a friend shared the template with the user
    sharedWithMe: boolean
    // the friends the template is shared with, only filled for its creator
    sharedWith: WorkoutUser[]
}

export interface WorkoutUser {
    id: string
    name: string | null
    image: string | null
}

export type WorkoutExercise = StrengthExercise | CardioExercise

export interface BaseWorkoutExercise {
    type: WorkoutExerciseType
    exercise: Exercise
}

export interface StrengthExercise extends BaseWorkoutExercise {
    type: WorkoutExerciseType.STRENGTH_EXERCISE
    sets: ExerciseSet[]
}

// cardio like running or rowing, only with its minutes
export interface CardioExercise extends BaseWorkoutExercise {
    type: WorkoutExerciseType.CARDIO_EXERCISE
    minutes: number
    // always empty, older apps expect sets
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
    // a short note of the user, e.g. the seat height, null when there is none
    note: string | null
    sets: SessionSet[]
    // minutes of cardio like running or rowing, null for a strength exercise with sets
    cardio: SessionCardio | null
}

// only done cardio counts, like only done sets
export interface SessionCardio {
    minutes: number
    done: boolean
}

export interface SessionSet {
    reps: number
    weightKg: number
    rpe: number | null
    done: boolean
    // the rest after the set, null for the default
    restSeconds: number | null
}
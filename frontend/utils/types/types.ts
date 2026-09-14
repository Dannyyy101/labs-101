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
    rpe: number
}
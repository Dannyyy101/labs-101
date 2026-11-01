import type { Schemas } from "@/lib/api/types"

export interface WorkoutTemplate {
    id: string,
    name: string,
    type: string
}

// see WorkoutController in the backend
export type Exercise = Schemas["ExerciseDto"]
export type CreateExercise = Schemas["CreateExerciseDto"]

export interface BaseExercise extends Omit<Exercise, "type"> {
    type: ExerciseTypes
}

export interface StrengthTraining extends BaseExercise {
    type: "Strength Training"
    sets: Set[]
}

export interface SwimmingTraining extends BaseExercise {
    type: "Swimming"
}

export interface RunningTraining extends BaseExercise {
    type: "Running"
}

export interface StretchingTraining extends BaseExercise {
    type: "Stretching"
}

export interface Set {
    count: number
    weight: number
    unit: string
}

export type ExerciseTypes = "Strength Training" | "Swimming" | "Running" | "Stretching"
export type ExerciseTraining = StrengthTraining | SwimmingTraining | RunningTraining | StretchingTraining

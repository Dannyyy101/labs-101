import { ExtendedBodyPart } from "react-muscle-highlighter"

export interface WorkoutTemplate {
    id: string,
    name: string,
    type: string
}

export interface BaseExercise {
    id: number,
    name: string
    description: string
    type: ExerciseTypes
    bodyParts: ExtendedBodyPart[]
}

export interface CreateExercise {
    name: string
    description: string
    type: string
    bodyParts: ExtendedBodyPart[]
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
export type Exercise = BaseExercise
export type ExerciseTraining = StrengthTraining | SwimmingTraining | RunningTraining | StretchingTraining

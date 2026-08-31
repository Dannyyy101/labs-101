import { ExtendedBodyPart } from "react-muscle-highlighter"

export interface WorkoutTemplate {
    id: string,
    name: string,
    type: string
}

export interface Exercise {
    id: number,
    name: string
    description: string
    type: string
    bodyParts: ExtendedBodyPart[]
}

export interface CreateExercise {
    name: string
    description: string
    type: string
    bodyParts: ExtendedBodyPart[]
}

export interface StrengthTraining extends Exercise {
    defaultSets: Set[]
}

interface Set {
    count: number
    weight: number
    unit: string
}

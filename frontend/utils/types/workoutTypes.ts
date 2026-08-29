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
}

export interface CreateExercise {
    name: string
    description: string
    type: string
}
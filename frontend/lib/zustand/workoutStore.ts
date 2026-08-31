import { Exercise } from "@/utils/types/workoutTypes"
import { create } from "zustand"


export interface WorkoutState {
    exercises: Exercise[]
    setExercises: (exercises: Exercise[]) => void
}

export const useExercisesStore = create<WorkoutState>((set) => ({
    exercises: [],
    setExercises: ((exercises) => set(() => ({ exercises: exercises })))
}))
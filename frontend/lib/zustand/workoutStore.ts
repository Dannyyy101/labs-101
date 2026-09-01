import { Exercise, ExerciseTraining } from "@/utils/types/workoutTypes"
import { create } from "zustand"


export interface WorkoutState {
    exercises: ExerciseTraining[]
    setExercises: (exercises: ExerciseTraining[]) => void
    updateExercise: (exercise: ExerciseTraining) => void
}

export const useExercisesStore = create<WorkoutState>((set) => ({
    exercises: [],
    setExercises: ((exercises) => set(() => ({ exercises: exercises }))),
    updateExercise: ((exercise) => set((e) => ({ exercises: e.exercises.map((ex) => ex.id === exercise.id ? { ...exercise } : ex) })
    )),
}))
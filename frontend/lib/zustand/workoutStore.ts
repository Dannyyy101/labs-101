import { WorkoutExercise } from "@/utils/types/types"
import { ExerciseTraining } from "@/utils/types/workoutTypes"
import { create } from "zustand"


export interface WorkoutState {
    exercises: WorkoutExercise[]
    setExercises: (exercises: WorkoutExercise[]) => void
    updateExercise: (exercise: WorkoutExercise) => void
}

export const useExercisesStore = create<WorkoutState>((set) => ({
    exercises: [],
    setExercises: ((exercises) => set(() => ({ exercises: exercises }))),
    updateExercise: ((exercise) => set((e) => ({ exercises: e.exercises.map((ex) => ex.exercise.id === exercise.exercise.id ? { ...exercise } : ex) })
    )),
}))
'use server'

import { api } from "@/lib/api/client"
import { CreateExercise, Exercise } from "@/utils/types/workoutTypes"
import { revalidatePath } from "next/cache"

export async function getAllExercises(filter?: { type?: string, name?: string }): Promise<Exercise[]> {
    const { data } = await api.GET("/api/workouts/exercises", {
        params: { query: { name: filter?.name || undefined, type: filter?.type || undefined } },
    })

    if (!data) throw new Error("Error fetching exercises")
    return data
}

export async function createExercise(exercise: CreateExercise): Promise<void> {
    const { response } = await api.POST("/api/workouts/exercises", { body: exercise })

    if (!response.ok) throw new Error("Error creating exercise")
    revalidatePath("/exercises")
}

export async function updateExercise(exercise: Exercise): Promise<void> {
    const { response } = await api.PUT("/api/workouts/exercises/{id}", {
        params: { path: { id: String(exercise.id) } }, body: exercise,
    })

    if (!response.ok) throw new Error("Error updating exercise")
    revalidatePath("/exercises")
}

export async function deleteExercise(id: number): Promise<void> {
    const { response } = await api.DELETE("/api/workouts/exercises/{id}", { params: { path: { id: String(id) } } })

    if (!response.ok) throw new Error("Error deleting exercise")
    revalidatePath("/exercises")
}

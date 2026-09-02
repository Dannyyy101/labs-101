'use server'

import { BACKEND_URL } from "@/utils/constants";
import { CreateExercise, Exercise } from "@/utils/types/workoutTypes";
import { revalidatePath } from "next/cache";

export async function getAllExercises(filter?: { type?: string, name?: string }): Promise<Exercise[]> {
    const url = new URL(BACKEND_URL + "/workouts/exercises")
    if (filter) {
        if (filter.name)
            url.searchParams.append("name", filter.name)
        if (filter.type)
            url.searchParams.append("type", filter.type)
    }
    const response = await fetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Exercise[]
    }

    throw new Error("Error fetching exercises")
}

export async function createExercise(exercise: CreateExercise): Promise<void> {

    const response = await fetch(BACKEND_URL + "/workouts/exercises", {
        method: "POST", body: JSON.stringify(exercise), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (response.ok) {
        revalidatePath("/exercises")
        return
    }

    throw new Error("Error creating exercise")
}


export async function updateExercise(exercise: Exercise): Promise<void> {
    const response = await fetch(BACKEND_URL + "/workouts/exercises/" + exercise.id, {
        method: "PUT", body: JSON.stringify(exercise), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (response.ok) {
        revalidatePath("/exercises")
        return
    }

    throw new Error("Error creating exercise")
}

export async function deleteExercise(id: number): Promise<void> {
    const response = await fetch(BACKEND_URL + "/workouts/exercises/" + id, {
        method: "DELETE",
    })

    if (response.ok) {
        revalidatePath("/exercises")
        return
    }

    throw new Error("Error creating exercise")
}
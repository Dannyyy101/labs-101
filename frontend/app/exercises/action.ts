'use server'

import { BACKEND_URL } from "@/utils/constants";
import { CreateExercise, Exercise } from "@/utils/types/workoutTypes";
import { revalidatePath } from "next/cache";

export async function getAllExercises(): Promise<Exercise[]> {
    const response = await fetch(BACKEND_URL + "/workouts/exercises", { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Exercise[]
    }

    throw new Error("Error fetching exercises")
}

export async function createExercise(formData: FormData): Promise<void> {
    const name = formData.get("exercise-name")?.toString() || ""
    const description = formData.get("exercise-description")?.toString() || ""
    const type = formData.get("exercise-type")?.toString() || ""


    const response = await fetch(BACKEND_URL + "/workouts/exercises", {
        method: "POST", body: JSON.stringify({ name, description, type } as CreateExercise), headers: {
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
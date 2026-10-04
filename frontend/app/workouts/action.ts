'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { Workout } from "@/utils/types/types"

export async function getAllWorkouts(): Promise<Workout[]> {
    const url = new URL(BACKEND_URL + "/workouts")

    const response = await backendFetch(url.toString(), { cache: "no-cache" })

    if (response.ok) {
        return await response.json() as Workout[]
    }

    throw new Error("Error fetching workouts")
}

export async function getWorkoutById(id: number): Promise<Workout> {
    const url = new URL(`${BACKEND_URL}/workouts/${id}`)

    const response = await backendFetch(url.toString(), { cache: "no-cache" })

    if (response.ok) {
        return await response.json() as Workout
    }

    throw new Error(`Error fetching workout with id ${id}`)
}
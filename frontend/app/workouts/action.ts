'use server'

import { BACKEND_URL } from "@/utils/constants"
import { Workout } from "@/utils/types/types"

export async function getAllWorkouts(): Promise<Workout[]> {
    const url = new URL(BACKEND_URL + "/workouts")

    const response = await fetch(url.toString(), { cache: "no-cache" })

    if (response.ok) {
        return await response.json() as Workout[]
    }

    throw new Error("Error fetching workouts")
}

export async function getWorkoutById(id: number): Promise<Workout> {
    const url = new URL(`${BACKEND_URL}/workouts/${id}`)

    const response = await fetch(url.toString(), { cache: "no-cache" })

    if (response.ok) {
        return await response.json() as Workout
    }

    throw new Error(`Error fetching workout with id ${id}`)
}
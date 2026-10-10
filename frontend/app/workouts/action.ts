'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { SessionExercise, Workout, WorkoutExercise, WorkoutSession } from "@/utils/types/types"
import { revalidatePath } from "next/cache"

const JSON_HEADERS = { 'Content-Type': 'application/json' }

const sessionsUrl = async (path = "") => `${BACKEND_URL}/users/me/workout-sessions${path}`

// MARK: templates

export async function getAllWorkouts(): Promise<Workout[]> {
    const url = new URL(BACKEND_URL + "/workouts")

    const response = await backendFetch(url.toString(), { cache: "no-store" })

    if (response.ok) {
        return await response.json() as Workout[]
    }

    throw new Error("Error fetching workouts")
}

export async function getWorkoutById(id: number): Promise<Workout> {
    const url = new URL(`${BACKEND_URL}/workouts/${id}`)

    const response = await backendFetch(url.toString(), { cache: "no-store" })

    if (response.ok) {
        return await response.json() as Workout
    }

    throw new Error(`Error fetching workout with id ${id}`)
}

/** Creates the workout without an id, otherwise replaces it. */
export async function saveWorkout(workout: { id: number | null, name: string, workoutExercises: WorkoutExercise[] }): Promise<Workout> {
    const body = JSON.stringify({
        name: workout.name,
        exercises: workout.workoutExercises.map((e) => ({
            ...e,
            sets: e.sets.map((set, order) => ({ ...set, order })),
        })),
    })
    const response = workout.id == null
        ? await backendFetch(`${BACKEND_URL}/workouts`, { method: "POST", body, headers: JSON_HEADERS })
        : await backendFetch(`${BACKEND_URL}/workouts/${workout.id}`, { method: "PUT", body, headers: JSON_HEADERS })

    if (response.ok) {
        revalidatePath("/workouts")
        return await response.json() as Workout
    }

    throw new Error("Error saving workout")
}

export async function deleteWorkout(id: number): Promise<void> {
    const response = await backendFetch(`${BACKEND_URL}/workouts/${id}`, { method: "DELETE" })

    if (!response.ok) throw new Error(`Error deleting workout ${id}`)
    revalidatePath("/workouts")
}

// MARK: sessions

/** Finished workouts started in the range, newest first. */
export async function getSessions(from: Date, to: Date): Promise<WorkoutSession[]> {
    const url = new URL(await sessionsUrl())
    url.searchParams.append("from", from.toISOString())
    url.searchParams.append("to", to.toISOString())

    const response = await backendFetch(url.toString(), { cache: "no-store" })

    if (response.ok) {
        return await response.json() as WorkoutSession[]
    }

    throw new Error("Error fetching workout sessions")
}

export async function getActiveSession(): Promise<WorkoutSession | null> {
    const response = await backendFetch(await sessionsUrl("/active"), { cache: "no-store" })

    if (response.status === 204) return null
    if (response.ok) {
        return await response.json() as WorkoutSession
    }

    throw new Error("Error fetching the active workout session")
}

/** Starts a workout from the template (or an empty one), nothing happens while another one is running. */
export async function startSession(workoutId: number | null): Promise<void> {
    const response = await backendFetch(await sessionsUrl(), {
        method: "POST", body: JSON.stringify({ workoutId }), headers: JSON_HEADERS
    })

    // 400: another workout is still running, the caller opens that one instead
    if (!response.ok && response.status !== 400) throw new Error("Error starting the workout")
    revalidatePath("/workouts")
}

export async function updateSession(id: number, name: string, exercises: SessionExercise[]): Promise<void> {
    const response = await backendFetch(await sessionsUrl(`/${id}`), {
        method: "PUT", body: JSON.stringify({ name, exercises }), headers: JSON_HEADERS
    })

    if (!response.ok) throw new Error(`Error saving workout session ${id}`)
}

/** Saves the last state and ends the workout, sets that weren't checked off are dropped. */
export async function finishSession(id: number, name: string, exercises: SessionExercise[]): Promise<void> {
    const response = await backendFetch(await sessionsUrl(`/${id}/finish`), {
        method: "POST", body: JSON.stringify({ name, exercises }), headers: JSON_HEADERS
    })

    if (!response.ok) throw new Error(`Error finishing workout session ${id}`)
    revalidatePath("/workouts")
}

export async function deleteSession(id: number): Promise<void> {
    const response = await backendFetch(await sessionsUrl(`/${id}`), { method: "DELETE" })

    if (!response.ok) throw new Error(`Error deleting workout session ${id}`)
    revalidatePath("/workouts")
}

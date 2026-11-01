'use server'

import { api } from "@/lib/api/client"
import { SessionExercise, Workout, WorkoutExercise, WorkoutSession } from "@/utils/types/types"
import { revalidatePath } from "next/cache"

// MARK: templates

export async function getAllWorkouts(): Promise<Workout[]> {
    const { data } = await api.GET("/api/workouts")

    if (!data) throw new Error("Error fetching workouts")
    return data
}

export async function getWorkoutById(id: number): Promise<Workout> {
    const { data } = await api.GET("/api/workouts/{id}", { params: { path: { id } } })

    if (!data) throw new Error(`Error fetching workout with id ${id}`)
    return data
}

/** Creates the workout without an id, otherwise replaces it. */
export async function saveWorkout(workout: { id: number | null, name: string, workoutExercises: WorkoutExercise[] }): Promise<Workout> {
    const body = {
        name: workout.name,
        exercises: workout.workoutExercises.map((e) => ({
            ...e,
            sets: e.sets.map((set, order) => ({ ...set, order })),
        })),
    }
    const { data } = workout.id == null
        ? await api.POST("/api/workouts", { body })
        : await api.PUT("/api/workouts/{id}", { params: { path: { id: workout.id } }, body })

    if (!data) throw new Error("Error saving workout")
    revalidatePath("/workouts")
    return data
}

/** Shares the template with exactly these friends, only its creator can. */
export async function shareWorkout(id: number, userIds: string[]): Promise<Workout> {
    const { data } = await api.PUT("/api/workouts/{id}/shares", { params: { path: { id } }, body: { userIds } })

    if (!data) throw new Error(`Error sharing workout ${id}`)
    revalidatePath("/workouts")
    return data
}

/** Removes a template a friend shared from the own list. */
export async function leaveWorkout(id: number): Promise<void> {
    const { response } = await api.DELETE("/api/workouts/{id}/shares/me", { params: { path: { id } } })

    if (!response.ok) throw new Error(`Error removing shared workout ${id}`)
    revalidatePath("/workouts")
}

export async function deleteWorkout(id: number): Promise<void> {
    const { response } = await api.DELETE("/api/workouts/{id}", { params: { path: { id } } })

    if (!response.ok) throw new Error(`Error deleting workout ${id}`)
    revalidatePath("/workouts")
}

// MARK: sessions

/** Finished workouts started in the range, newest first. */
export async function getSessions(from: Date, to: Date): Promise<WorkoutSession[]> {
    const { data } = await api.GET("/api/users/me/workout-sessions", {
        params: { query: { from: from.toISOString(), to: to.toISOString() } },
    })

    if (!data) throw new Error("Error fetching workout sessions")
    return data
}

export async function getActiveSession(): Promise<WorkoutSession | null> {
    const { data, response } = await api.GET("/api/users/me/workout-sessions/active")

    if (!response.ok) throw new Error("Error fetching the active workout session")
    // 204 while no workout is running
    return data ?? null
}

/** Starts a workout from the template (or an empty one), nothing happens while another one is running. */
export async function startSession(workoutId: number | null): Promise<void> {
    const { response } = await api.POST("/api/users/me/workout-sessions", { body: { workoutId } })

    // 400: another workout is still running, the caller opens that one instead
    if (!response.ok && response.status !== 400) throw new Error("Error starting the workout")
    revalidatePath("/workouts")
}

export async function updateSession(id: number, name: string, exercises: SessionExercise[]): Promise<void> {
    const { response } = await api.PUT("/api/users/me/workout-sessions/{id}", {
        params: { path: { id } }, body: { name, exercises },
    })

    if (!response.ok) throw new Error(`Error saving workout session ${id}`)
}

/** Saves the last state and ends the workout, sets that weren't checked off are dropped. */
export async function finishSession(id: number, name: string, exercises: SessionExercise[]): Promise<void> {
    const { response } = await api.POST("/api/users/me/workout-sessions/{id}/finish", {
        params: { path: { id } }, body: { name, exercises },
    })

    if (!response.ok) throw new Error(`Error finishing workout session ${id}`)
    revalidatePath("/workouts")
}

export async function deleteSession(id: number): Promise<void> {
    const { response } = await api.DELETE("/api/users/me/workout-sessions/{id}", { params: { path: { id } } })

    if (!response.ok) throw new Error(`Error deleting workout session ${id}`)
    revalidatePath("/workouts")
}

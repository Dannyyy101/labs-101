'use server'

import { mapFromCalendarEventDto } from "@/lib/mapper/calendarMapper"
import { BACKEND_URL } from "@/utils/constants"
import { CalendarEvent, CalendarEventDto } from "@/utils/types/calendarTypes"
import { Result } from "@/utils/types/types"
import { Exercise, WorkoutTemplate } from "@/utils/types/workoutTypes"
import { sortWorkoutTemplates } from "@/utils/workout"
import { revalidatePath } from "next/cache"

export async function getAllWorkoutTemplates(): Promise<Result<Map<string, WorkoutTemplate[]>>> {
    const response = await fetch(BACKEND_URL + "/workouts/templates")

    if (response.ok) {
        try {
            const templates = await response.json() as WorkoutTemplate[]
            return { value: sortWorkoutTemplates(templates), error: null }
        } catch (e) {
            return { value: null, error: e as Error }
        }
    }

    return { value: null, error: new Error("Error fetching workout templates") }
}

export async function getAllExercises(): Promise<Exercise[]> {
    const response = await fetch(BACKEND_URL + "/workouts/exercises")

    if (response.ok) {
        return await response.json() as Exercise[]
    }

    throw new Error("Error fetching workout templates")
}

export async function getAllCalendarEvents(dateRange: Date, steps: number): Promise<Result<CalendarEvent[]>> {
    const url = new URL(BACKEND_URL + "/calendar")
    url.searchParams.append("startDate", dateRange.toISOString())
    const endDate = new Date(dateRange)
    endDate.setDate(endDate.getDate() + steps)
    url.searchParams.append("endDate", endDate.toISOString())

    const response = await fetch(url.toString())

    if (response.ok) {
        try {
            const events = await response.json() as CalendarEventDto[]
            return { value: events.map((e) => mapFromCalendarEventDto(e)), error: null }
        } catch (e) {
            return { value: null, error: e as Error }
        }
    }

    return { value: null, error: new Error("Error fetching calendar events") }
}

export async function createCalendarEvent(calendarEvent: CalendarEvent): Promise<void> {
    const response = await fetch(BACKEND_URL + "/calendar", {
        method: "POST", body: JSON.stringify({ ...calendarEvent, startDate: calendarEvent.startDate.toISOString(), endDate: calendarEvent.endDate.toISOString() }), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (response.ok) {
        revalidatePath("/planner")
        return
    }

    throw new Error("Error creating calendar events")
}
export async function updateCalendarEvent(calendarEvent: CalendarEvent): Promise<Result<void>> {
    const response = await fetch(BACKEND_URL + "/calendar/" + calendarEvent.id, {
        method: "PUT", body: JSON.stringify({ ...calendarEvent, startDate: calendarEvent.startDate.toISOString(), endDate: calendarEvent.endDate.toISOString() }), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (response.ok) {
        try {
            return { value: null, error: null }
        } catch (e) {
            return { value: null, error: e as Error }
        }
    }

    return { value: null, error: new Error("Error fetching calendar events") }
}


export async function deleteCalendarEvent(id: string): Promise<Result<void>> {
    const response = await fetch(BACKEND_URL + "/calendar/" + id, {
        method: "DELETE"
    })

    if (response.ok) {
        try {
            return { value: null, error: null }
        } catch (e) {
            return { value: null, error: e as Error }
        }
    }

    return { value: null, error: new Error(`Error deleting calendar event with id: ${id}`) }
}
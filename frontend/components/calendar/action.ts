'use server'

import { mapFromCalendarEventDto } from "@/lib/mapper/calendarMapper"
import { BACKEND_URL } from "@/utils/constants"
import { CalendarEvent, CalendarEventDto } from "@/utils/types/calendarTypes"
import { Result } from "@/utils/types/types"
import { WorkoutTemplate } from "@/utils/types/workoutTypes"
import { sortWorkoutTemplates } from "@/utils/workout"

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

export async function getAllCalendarEvents(): Promise<Result<CalendarEvent[]>> {
    const response = await fetch(BACKEND_URL + "/calendar")

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

export async function createCalendarEvent(calendarEvent: CalendarEvent): Promise<Result<void>> {
    const response = await fetch(BACKEND_URL + "/calendar", {
        method: "POST", body: JSON.stringify({...calendarEvent, startDate: calendarEvent.startDate.toISOString(), endDate: calendarEvent.endDate.toISOString() }), headers: {
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

export async function deleteCalendarEvent(id:string): Promise<Result<void>> {
    const response = await fetch(BACKEND_URL + "/calendar/" + id , {
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
'use server'

import { api } from "@/lib/api/client"
import { mapFromCalendarEventDto, mapFromExternalCalendarEventDto } from "@/lib/mapper/calendarMapper"
import { CalendarEvent, CalendarUser } from "@/utils/types/calendarTypes"
import { Result } from "@/utils/types/types"
import { Exercise } from "@/utils/types/workoutTypes"
import { revalidatePath } from "next/cache"

export async function getAllExercises(): Promise<Exercise[]> {
    const { data } = await api.GET("/api/workouts/exercises")

    if (!data) throw new Error("Error fetching exercises")
    return data
}

const range = (dateRange: Date, steps: number) => {
    const endDate = new Date(dateRange)
    endDate.setDate(endDate.getDate() + steps)
    return { startDate: dateRange.toISOString(), endDate: endDate.toISOString() }
}

export async function getAllCalendarEvents(dateRange: Date, steps: number): Promise<Result<CalendarEvent[]>> {
    // only the user's own events and the ones they were invited to
    const { data } = await api.GET("/api/calendar", { params: { query: range(dateRange, steps) } })

    if (!data) return { value: null, error: new Error("Error fetching calendar events") }
    return { value: data.map(mapFromCalendarEventDto), error: null }
}

/** The stored events of the user's Apple calendars, empty when there are none or they can't be loaded. */
export async function getExternalCalendarEvents(dateRange: Date, steps: number): Promise<CalendarEvent[]> {
    try {
        const { data, response } = await api.GET("/api/calendar/external/events", { params: { query: range(dateRange, steps) } })
        if (data) return data.map(mapFromExternalCalendarEventDto)
        console.error(`Error fetching external calendar events: ${response.status}`)
    } catch (e) {
        console.error("Error fetching external calendar events", e)
    }
    // the own events are still shown
    return []
}

/** Loads the events of the user's Apple calendars from Apple again, null when that didn't work. */
export async function syncExternalCalendarEvents(dateRange: Date, steps: number): Promise<CalendarEvent[] | null> {
    try {
        const { data, response } = await api.POST("/api/calendar/external/events/sync", { params: { query: range(dateRange, steps) } })
        if (data) return data.map(mapFromExternalCalendarEventDto)
        console.error(`Error syncing external calendar events: ${response.status}`)
    } catch (e) {
        console.error("Error syncing external calendar events", e)
    }
    return null
}

/** The ids of the invitees instead of the people, the backend only needs those. */
const toRequestBody = (calendarEvent: CalendarEvent) => ({
    title: calendarEvent.title,
    startDate: calendarEvent.startDate.toISOString(),
    endDate: calendarEvent.endDate.toISOString(),
    creatorId: calendarEvent.creatorId,
    workoutId: calendarEvent.workoutId,
    inviteeIds: calendarEvent.invitees.map((user) => user.id),
    recurrence: calendarEvent.recurrence,
    recurrenceEnd: calendarEvent.recurrenceEnd?.toISOString() ?? null,
})

/** People to invite, matched by name or email. */
export async function searchUsers(query: string): Promise<CalendarUser[]> {
    const { data } = await api.GET("/api/users", { params: { query: { query } } })

    if (!data) throw new Error("Error searching users")
    return data
}

export async function createCalendarEvent(calendarEvent: CalendarEvent): Promise<CalendarEvent> {
    const { data, response } = await api.POST("/api/calendar", { body: toRequestBody(calendarEvent) })

    if (!data) {
        console.error(response.status, response.statusText)
        throw new Error("Error creating calendar events")
    }
    revalidatePath("/planner")
    return mapFromCalendarEventDto(data)
}

/** {@link occurrenceStartDate} is the start of the edited occurrence of a repeating event, the whole series is moved like it. */
export async function updateCalendarEvent(calendarEvent: CalendarEvent, occurrenceStartDate?: Date): Promise<Result<void>> {
    const id = Number(calendarEvent.id)
    const { response } = await api.PUT("/api/calendar/{id}", {
        params: { path: { id } },
        body: { ...toRequestBody(calendarEvent), id, occurrenceStartDate: occurrenceStartDate?.toISOString() ?? null },
    })

    if (!response.ok) return { value: null, error: new Error("Error updating calendar event") }
    return { value: null, error: null }
}

export async function deleteCalendarEvent(id: string): Promise<Result<void>> {
    const { response } = await api.DELETE("/api/calendar/{id}", { params: { path: { id: Number(id) } } })

    if (!response.ok) return { value: null, error: new Error(`Error deleting calendar event with id: ${id}`) }
    return { value: null, error: null }
}

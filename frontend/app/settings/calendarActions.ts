'use server'

import { api, toResult } from "@/lib/api/client"
import { ExternalCalendar } from "@/utils/types/calendarTypes"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

// the settings and the planner show the calendars
function changed<T>(result: Result<T>): Result<T> {
    if (result.ok) {
        revalidatePath("/settings")
        revalidatePath("/planner")
    }
    return result
}

export async function getExternalCalendars(): Promise<Result<ExternalCalendar[]>> {
    return toResult(() => api.GET("/api/calendar/external"), "Kalender konnten nicht geladen werden")
}

/** The backend loads the calendar once, a link that gives no calendar is refused with a message. */
export async function addExternalCalendar(calendar: { name: string, url: string }): Promise<Result<ExternalCalendar>> {
    return changed(await toResult(() => api.POST("/api/calendar/external", { body: calendar }),
        "Kalender konnte nicht hinzugefügt werden"))
}

export async function deleteExternalCalendar(id: number): Promise<Result<void>> {
    return changed(await toResult(() => api.DELETE("/api/calendar/external/{id}", { params: { path: { id } } }),
        "Kalender konnte nicht entfernt werden"))
}

/** Signs in to iCloud with an app-specific password, all calendars of the user afterwards. */
export async function connectAppleAccount(account: { appleId: string, password: string }): Promise<Result<ExternalCalendar[]>> {
    return changed(await toResult(() => api.POST("/api/calendar/external/apple", { body: account }),
        "Apple-ID konnte nicht verbunden werden"))
}

export async function disconnectAppleAccount(accountId: number): Promise<Result<void>> {
    return changed(await toResult(() => api.DELETE("/api/calendar/external/apple/{accountId}", { params: { path: { accountId } } }),
        "Apple-ID konnte nicht getrennt werden"))
}

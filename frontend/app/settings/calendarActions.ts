'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { ApiError } from "@/utils/types/api"
import { ExternalCalendar } from "@/utils/types/calendarTypes"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

const URL = `${BACKEND_URL}/calendar/external`

// the localized message of the backend if there is one
async function errorMessage(response: Response, fallback: string): Promise<string> {
    const apiError = await response.json().catch(() => null) as ApiError | null
    return apiError?.errorMessage || fallback
}

export async function getExternalCalendars(): Promise<Result<ExternalCalendar[]>> {
    try {
        const response = await backendFetch(URL, { cache: 'no-store' })
        if (response.ok) {
            return { ok: true, data: await response.json() as ExternalCalendar[] }
        }
        console.error(`Error fetching external calendars: ${response.status}`)
        return { ok: false, error: await errorMessage(response, "Kalender konnten nicht geladen werden") }
    } catch (e) {
        console.error("Error fetching external calendars", e)
        return { ok: false, error: "Kalender konnten nicht geladen werden, der Server ist nicht erreichbar" }
    }
}

/** The backend loads the calendar once, a link that gives no calendar is refused with a message. */
export async function addExternalCalendar(calendar: { name: string, url: string }): Promise<Result<ExternalCalendar>> {
    try {
        const response = await backendFetch(URL, {
            method: "POST",
            body: JSON.stringify(calendar),
            headers: { 'Content-Type': 'application/json' },
            cache: 'no-store'
        })
        if (response.ok) {
            revalidatePath("/settings")
            revalidatePath("/planner")
            return { ok: true, data: await response.json() as ExternalCalendar }
        }
        return { ok: false, error: await errorMessage(response, "Kalender konnte nicht hinzugefügt werden") }
    } catch (e) {
        console.error("Error adding external calendar", e)
        return { ok: false, error: "Kalender konnte nicht hinzugefügt werden, der Server ist nicht erreichbar" }
    }
}

export async function deleteExternalCalendar(id: number): Promise<Result<null>> {
    try {
        const response = await backendFetch(`${URL}/${id}`, { method: "DELETE", cache: 'no-store' })
        if (response.ok) {
            revalidatePath("/settings")
            revalidatePath("/planner")
            return { ok: true, data: null }
        }
        return { ok: false, error: await errorMessage(response, "Kalender konnte nicht entfernt werden") }
    } catch (e) {
        console.error("Error deleting external calendar", e)
        return { ok: false, error: "Kalender konnte nicht entfernt werden, der Server ist nicht erreichbar" }
    }
}

/** Signs in to iCloud with an app-specific password, all calendars of the user afterwards. */
export async function connectAppleAccount(account: { appleId: string, password: string }): Promise<Result<ExternalCalendar[]>> {
    try {
        const response = await backendFetch(`${URL}/apple`, {
            method: "POST",
            body: JSON.stringify(account),
            headers: { 'Content-Type': 'application/json' },
            cache: 'no-store'
        })
        if (response.ok) {
            revalidatePath("/settings")
            revalidatePath("/planner")
            return { ok: true, data: await response.json() as ExternalCalendar[] }
        }
        return { ok: false, error: await errorMessage(response, "Apple-ID konnte nicht verbunden werden") }
    } catch (e) {
        console.error("Error connecting Apple account", e)
        return { ok: false, error: "Apple-ID konnte nicht verbunden werden, der Server ist nicht erreichbar" }
    }
}

export async function disconnectAppleAccount(accountId: number): Promise<Result<null>> {
    try {
        const response = await backendFetch(`${URL}/apple/${accountId}`, { method: "DELETE", cache: 'no-store' })
        if (response.ok) {
            revalidatePath("/settings")
            revalidatePath("/planner")
            return { ok: true, data: null }
        }
        return { ok: false, error: await errorMessage(response, "Apple-ID konnte nicht getrennt werden") }
    } catch (e) {
        console.error("Error disconnecting Apple account", e)
        return { ok: false, error: "Apple-ID konnte nicht getrennt werden, der Server ist nicht erreichbar" }
    }
}

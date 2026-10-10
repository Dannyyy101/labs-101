'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { ApiError } from "@/utils/types/api"
import { Result } from "@/utils/types/result"
import { UserSettings } from "@/utils/types/settings"
import { revalidatePath } from "next/cache"

// settings are optional, so none of these actions throw: a failing backend must not take down
// the pages using them, they get a Result and decide themselves what to show

async function settingsUrl(): Promise<string> {

    return `${BACKEND_URL}/users/me/settings`
}

// the localized message of the backend if there is one
async function errorMessage(response: Response, fallback: string): Promise<string> {
    const apiError = await response.json().catch(() => null) as ApiError | null
    return apiError?.errorMessage || fallback
}

// data is null as long as the user hasn't saved any settings
export async function getSettings(): Promise<Result<UserSettings | null>> {
    try {
        const response = await backendFetch(await settingsUrl(), { cache: 'no-store' })

        if (response.ok) {
            return { ok: true, data: await response.json() as UserSettings }
        }
        if (response.status === 404) {
            return { ok: true, data: null }
        }

        console.error(`Error fetching settings: ${response.status}`)
        return { ok: false, error: await errorMessage(response, "Einstellungen konnten nicht geladen werden") }
    } catch (e) {
        // backend not reachable
        console.error("Error fetching settings", e)
        return { ok: false, error: "Einstellungen konnten nicht geladen werden, der Server ist nicht erreichbar" }
    }
}

// updates the settings, creates them on the first save
export async function saveSettings(settings: { calorieGoal: number }): Promise<Result<UserSettings>> {
    try {
        const url = await settingsUrl()
        const request = (method: "POST" | "PUT") => backendFetch(url, {
            method,
            body: JSON.stringify(settings),
            headers: {
                'Content-Type': 'application/json'
            },
            cache: 'no-store'
        })

        let response = await request("PUT")
        if (response.status === 404) {
            response = await request("POST")
        }

        if (response.ok) {
            revalidatePath("/settings")
            revalidatePath("/foods/track")
            return { ok: true, data: await response.json() as UserSettings }
        }

        console.error(`Error saving settings: ${response.status}`)
        return { ok: false, error: await errorMessage(response, "Einstellungen konnten nicht gespeichert werden") }
    } catch (e) {
        console.error("Error saving settings", e)
        return { ok: false, error: "Einstellungen konnten nicht gespeichert werden, der Server ist nicht erreichbar" }
    }
}

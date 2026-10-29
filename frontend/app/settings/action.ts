'use server'

import { api, errorMessage } from "@/lib/api/client"
import { Result } from "@/utils/types/result"
import { UserSettings } from "@/utils/types/settings"
import { revalidatePath } from "next/cache"

// settings are optional, so none of these actions throw: a failing backend must not take down
// the pages using them, they get a Result and decide themselves what to show

// data is null as long as the user hasn't saved any settings
export async function getSettings(): Promise<Result<UserSettings | null>> {
    try {
        const { data, error, response } = await api.GET("/api/users/me/settings")

        if (response.status === 404) {
            return { ok: true, data: null }
        }
        if (!data) {
            console.error(`Error fetching settings: ${response.status}`)
            return { ok: false, error: errorMessage(error, "Einstellungen konnten nicht geladen werden") }
        }
        return { ok: true, data }
    } catch (e) {
        // backend not reachable
        console.error("Error fetching settings", e)
        return { ok: false, error: "Einstellungen konnten nicht geladen werden, der Server ist nicht erreichbar" }
    }
}

// updates the settings, creates them on the first save
export async function saveSettings(settings: { calorieGoal: number }): Promise<Result<UserSettings>> {
    try {
        let answer = await api.PUT("/api/users/me/settings", { body: settings })
        if (answer.response.status === 404) {
            answer = await api.POST("/api/users/me/settings", { body: settings })
        }

        const { data, error, response } = answer
        if (!data) {
            console.error(`Error saving settings: ${response.status}`)
            return { ok: false, error: errorMessage(error, "Einstellungen konnten nicht gespeichert werden") }
        }
        revalidatePath("/settings")
        revalidatePath("/foods/track")
        return { ok: true, data }
    } catch (e) {
        console.error("Error saving settings", e)
        return { ok: false, error: "Einstellungen konnten nicht gespeichert werden, der Server ist nicht erreichbar" }
    }
}

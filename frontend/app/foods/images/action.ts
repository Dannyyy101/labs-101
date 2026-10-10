'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { ApiError } from "@/utils/types/api"
import { AcceptFoodImage, FoodImageAnalysis, FoodImageUsage } from "@/utils/types/foodImage"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

const imagesUrl = (path = "") => `${BACKEND_URL}/users/me/food-images${path}`

// the localized message of the backend if there is one, e.g. for the daily limit
async function errorMessage(response: Response, fallback: string): Promise<string> {
    const apiError = await response.json().catch(() => null) as ApiError | null
    return apiError?.errorMessage || fallback
}

async function request<T>(path: string, init: RequestInit, failure: string): Promise<Result<T>> {
    try {
        const response = await backendFetch(imagesUrl(path), { ...init, cache: 'no-store' })
        if (response.ok) {
            return { ok: true, data: await response.json() as T }
        }
        console.error(`${failure}: ${response.status}`)
        return { ok: false, error: await errorMessage(response, failure) }
    } catch (e) {
        console.error(failure, e)
        return { ok: false, error: `${failure}, der Server ist nicht erreichbar` }
    }
}

// formData with "image" (the photo), "meal" and optionally "description" (hint for the model)
export async function uploadFoodImage(formData: FormData): Promise<Result<FoodImageAnalysis>> {
    return request("", { method: "POST", body: formData }, "Foto konnte nicht hochgeladen werden")
}

export async function getFoodImageUsage(): Promise<Result<FoodImageUsage>> {
    return request("/usage", {}, "Tageslimit konnte nicht geladen werden")
}

export async function getFoodImages(limit = 5): Promise<Result<FoodImageAnalysis[]>> {
    return request(`?limit=${limit}`, {}, "Fotos konnten nicht geladen werden")
}

export async function getFoodImage(id: number): Promise<Result<FoodImageAnalysis>> {
    return request(`/${id}`, {}, "Foto konnte nicht geladen werden")
}

export async function acceptFoodImage(id: number, accept: AcceptFoodImage): Promise<Result<FoodImageAnalysis>> {
    const result = await request<FoodImageAnalysis>(`/${id}/accept`, {
        method: "POST",
        body: JSON.stringify(accept),
        headers: { 'Content-Type': 'application/json' },
    }, "Lebensmittel konnten nicht eingetragen werden")
    if (result.ok) {
        revalidatePath("/foods/track")
        revalidatePath(`/foods/images/${id}`)
    }
    return result
}

export async function rejectFoodImage(id: number): Promise<Result<FoodImageAnalysis>> {
    const result = await request<FoodImageAnalysis>(`/${id}/reject`, { method: "POST" }, "Foto konnte nicht abgelehnt werden")
    if (result.ok) revalidatePath(`/foods/images/${id}`)
    return result
}

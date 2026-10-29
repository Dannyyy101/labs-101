'use server'

import { api, toResult } from "@/lib/api/client"
import { AcceptFoodImage, FoodImageAnalysis, FoodImageUsage } from "@/utils/types/foodImage"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

// formData with "image" (the photo), "meal" and optionally "description" (hint for the model)
export async function uploadFoodImage(formData: FormData): Promise<Result<FoodImageAnalysis>> {
    const description = formData.get("description")
    // only the photo is a part, meal and description are parameters
    const image = new FormData()
    image.append("image", formData.get("image") as Blob)
    return toResult(() => api.POST("/api/users/me/food-images", {
        params: { query: { meal: String(formData.get("meal")), description: description ? String(description) : undefined } },
        // the type of the spec only knows the file as string, the photo goes as it is
        body: { image: "" },
        bodySerializer: () => image,
    }), "Foto konnte nicht hochgeladen werden")
}

export async function getFoodImageUsage(): Promise<Result<FoodImageUsage>> {
    return toResult(() => api.GET("/api/users/me/food-images/usage"), "Tageslimit konnte nicht geladen werden")
}

export async function getFoodImages(limit = 5): Promise<Result<FoodImageAnalysis[]>> {
    return toResult(() => api.GET("/api/users/me/food-images", { params: { query: { limit } } }), "Fotos konnten nicht geladen werden")
}

export async function getFoodImage(id: number): Promise<Result<FoodImageAnalysis>> {
    return toResult(() => api.GET("/api/users/me/food-images/{id}", { params: { path: { id } } }), "Foto konnte nicht geladen werden")
}

export async function acceptFoodImage(id: number, accept: AcceptFoodImage): Promise<Result<FoodImageAnalysis>> {
    const result = await toResult(() => api.POST("/api/users/me/food-images/{id}/accept", { params: { path: { id } }, body: accept }),
        "Lebensmittel konnten nicht eingetragen werden")
    if (result.ok) {
        revalidatePath("/foods/track")
        revalidatePath(`/foods/images/${id}`)
    }
    return result
}

export async function rejectFoodImage(id: number): Promise<Result<FoodImageAnalysis>> {
    const result = await toResult(() => api.POST("/api/users/me/food-images/{id}/reject", { params: { path: { id } } }),
        "Foto konnte nicht abgelehnt werden")
    if (result.ok) revalidatePath(`/foods/images/${id}`)
    return result
}

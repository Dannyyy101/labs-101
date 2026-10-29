'use server'

import { api, errorMessage } from "@/lib/api/client"
import { CreateTrackedFood, ExtractedFood, Food, FoodPortion, FoodWithLastEntry, FoodWithPortion, SearchFood, TrackedFood } from "@/utils/types/food"
import { Slice } from "@/utils/types/page"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

export async function findFoodByNameAndUserId(name: string, filter?: { page?: number }): Promise<Slice<SearchFood>> {
    const { data } = await api.GET("/api/foods/search/byNameAndUser", {
        params: { query: { name, page: filter?.page || undefined } },
    })

    if (!data) throw new Error("Error fetching foods")
    return data
}

export async function getTrackedFood(date: Date): Promise<TrackedFood[]> {
    const { data } = await api.GET("/api/users/me/tracked-foods", { params: { query: { date: date.toISOString() } } })

    if (!data) throw new Error("Error fetching tracked foods")
    return data
}

// kcal burned by activity on the day (from Apple Health), 0 without health data
export async function getBurnedCalories(day: string): Promise<number> {
    const answer = await api.GET("/api/users/me/health/overview/energy", { params: { query: { date: day } } }).catch(() => null)

    return answer?.data?.activeEnergy ?? 0
}

export async function getTrackedFoodByFoodId(foodId: number): Promise<FoodWithLastEntry> {
    const { data } = await api.GET("/api/users/me/foods/{foodId}/last", { params: { path: { foodId } } })

    if (!data) throw new Error(`Error fetching food by ${foodId}`)
    return data
}

export async function getTrackedFoodByTrackedFoodId(trackedFoodId: number): Promise<TrackedFood> {
    const { data } = await api.GET("/api/users/me/tracked-foods/{trackedFoodId}", { params: { path: { trackedFoodId } } })

    if (!data) throw new Error(`Error fetching tracked food by ${trackedFoodId}`)
    return data
}

export async function updateTrackFood(foodWithAmount: CreateTrackedFood) {
    const { error, response } = await api.PUT("/api/users/me/tracked-foods/{trackedFoodId}", {
        params: { path: { trackedFoodId: foodWithAmount.id! } }, body: foodWithAmount,
    })

    if (!response.ok) throw new Error(errorMessage(error, "Error updating tracked food"))
    revalidatePath("/foods/track")
}

export async function createTrackFood(foodWithAmount: CreateTrackedFood) {
    const { error, response } = await api.POST("/api/foods/{id}/track", {
        params: { path: { id: foodWithAmount.foodId } }, body: foodWithAmount,
    })

    if (!response.ok) throw new Error(errorMessage(error, "Error tracking food"))
    revalidatePath("/foods/track")
}

export async function deleteTrackedFood(trackedFoodId: number) {
    const { error, response } = await api.DELETE("/api/users/me/tracked-foods/{trackedFoodId}", { params: { path: { trackedFoodId } } })

    if (!response.ok) throw new Error(errorMessage(error, "Error deleting tracked food"))
    revalidatePath("/foods/track")
}

export async function findAndSafeFoodIfNotExistByBarcode(code: string): Promise<Result<Food>> {
    const { data, error } = await api.GET("/api/foods/bar-code/{code}", { params: { path: { code } } })

    if (!data) return { ok: false, error: errorMessage(error, "") }
    return { ok: true, data }
}

export async function extractFoodsFromText(text: string): Promise<ExtractedFood[]> {
    const { data } = await api.POST("/api/foods/extract", { body: { text } })

    if (!data) throw new Error("Error extracting foods from text")
    return data
}

export async function addFoodPortion(foodId: number, portion: Omit<FoodPortion, "id">): Promise<FoodPortion[]> {
    const { response } = await api.POST("/api/foods/{id}/portions", { params: { path: { id: foodId } }, body: portion })

    if (!response.ok) throw new Error(`Error adding portion to food ${foodId}`)

    const food = await getTrackedFoodByFoodId(foodId)
    return food.portions
}

// only reads the open food, it gets imported when it is tracked
export async function getOpenFood(openFoodId: number): Promise<FoodWithPortion> {
    const { data } = await api.GET("/api/foods/open-food/{openFoodId}", { params: { path: { openFoodId } } })

    if (!data) throw new Error(`Error fetching open food ${openFoodId}`)
    // without id until it is imported
    return { ...data, id: data.id ?? 0 }
}

export async function importOpenFood(openFoodId: number): Promise<FoodWithPortion> {
    const { data } = await api.POST("/api/foods/open-food/{openFoodId}/import", { params: { path: { openFoodId } } })

    if (!data?.id) throw new Error(`Error importing open food ${openFoodId}`)
    return { ...data, id: data.id }
}

export async function trackOpenFood(openFoodId: number, trackedFood: Omit<CreateTrackedFood, "foodId">) {
    const { response } = await api.POST("/api/foods/open-food/{openFoodId}/track", {
        params: { path: { openFoodId } }, body: trackedFood,
    })

    if (!response.ok) throw new Error(`Error tracking open food ${openFoodId}`)
    revalidatePath("/foods/track")
}

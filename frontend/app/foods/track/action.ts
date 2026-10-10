'use server'

import { BACKEND_URL } from "@/utils/constants"
import { backendFetch } from "@/utils/backend"
import { ApiError } from "@/utils/types/api"
import { CreateFoodWithAmount, CreateTrackedFood, ExtractedFood, Food, FoodPortion, FoodWithPortion, FoodWithLastEntry, SearchFood, TrackedFood, TrackFoodForUser } from "@/utils/types/food"
import { Page } from "@/utils/types/page"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

export async function findFoodByNameAndUserId(name: string, filter?: { page?: number }): Promise<Page<SearchFood>> {
    const url = new URL(BACKEND_URL + "/foods/search/byNameAndUser")

    url.searchParams.append("name", name)
    if (filter?.page) {
        url.searchParams.append("page", String(filter.page))
    }

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Page<SearchFood>
    }

    throw new Error("Error fetching foods")
}

export async function getTrackedFood(date: Date): Promise<TrackedFood[]> {

    const url = new URL(`${BACKEND_URL}/users/me/tracked-foods`)

    url.searchParams.append("date", date.toISOString())

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as TrackedFood[]
    }

    throw new Error("Error fetching tracked foods")
}

export async function getTrackedFoodByFoodId(foodId: number): Promise<FoodWithLastEntry> {

    const url = new URL(`${BACKEND_URL}/users/me/foods/${foodId}/last`)

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as FoodWithLastEntry
    }

    throw new Error(`Error fetching food by ${foodId}`)
}

export async function getTrackedFoodByTrackedFoodId(trackedFoodId: number): Promise<TrackedFood> {

    const url = new URL(`${BACKEND_URL}/users/me/tracked-foods/${trackedFoodId}`)

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as TrackedFood
    }

    throw new Error(`Error fetching tracked food by ${trackedFoodId}`)
}

export async function updateTrackFood(foodWithAmount: CreateTrackedFood) {
    const url = new URL(`${BACKEND_URL}/users/me/tracked-foods/${foodWithAmount.id}`)

    const response = await backendFetch(url.toString(), {
        method: "PUT",
        body: JSON.stringify(foodWithAmount), headers: {
            'Content-Type': 'application/json'
        }
    })
    if (response.ok) {
        revalidatePath("/foods/track")
        return
    }
    throw new Error(await response.json())
}

export async function createTrackFood(foodWithAmount: CreateTrackedFood) {
    const url = new URL(`${BACKEND_URL}/foods/${foodWithAmount.foodId}/track`)

    console.log(url)
    const response = await backendFetch(url.toString(), {
        method: "POST",
        body: JSON.stringify(foodWithAmount), headers: {
            'Content-Type': 'application/json'
        }
    })
    if (response.ok) {
        revalidatePath("/foods/track")
        return
    }
    throw new Error(await response.json())
}

export async function deleteTrackedFood(trackedFoodId: number) {
    const url = new URL(`${BACKEND_URL}/users/me/tracked-foods/${trackedFoodId}`)
    const response = await backendFetch(url.toString(), {
        method: "DELETE",
    })
    if (response.ok) {
        revalidatePath("/foods/track")
        return
    }
    throw new Error(await response.json())
}

export async function findAndSafeFoodIfNotExistByBarcode(code: string): Promise<Result<Food>> {
    const url = new URL(`${BACKEND_URL}/foods/bar-code/${code}`)
    const response = await backendFetch(url.toString())

    if (response.ok) {
        return { ok: true, data: await response.json() as Food }
    }

    const apiError = await response.json().catch(() => null) as ApiError | null
    return {
        ok: false,
        error: apiError?.errorMessage ?? ""
    }
}
export async function extractFoodsFromText(text: string): Promise<ExtractedFood[]> {
    const url = new URL(`${BACKEND_URL}/foods/extract`)
    const response = await backendFetch(url.toString(), {
        method: "POST",
        body: JSON.stringify({ text }), headers: {
            'Content-Type': 'application/json'
        },
        cache: 'no-store'
    })

    if (response.ok) {
        return await response.json() as ExtractedFood[]
    }

    throw new Error("Error extracting foods from text")
}

export async function addFoodPortion(foodId: number, portion: Omit<FoodPortion, "id">): Promise<FoodPortion[]> {
    const url = new URL(`${BACKEND_URL}/foods/${foodId}/portions`)
    const response = await backendFetch(url.toString(), {
        method: "POST",
        body: JSON.stringify(portion), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (!response.ok) throw new Error(`Error adding portion to food ${foodId}`)

    const food = await getTrackedFoodByFoodId(foodId)
    return food.portions
}

// only reads the open food, it gets imported when it is tracked
export async function getOpenFood(openFoodId: number): Promise<FoodWithPortion> {
    const url = new URL(`${BACKEND_URL}/foods/open-food/${openFoodId}`)
    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as FoodWithPortion
    }

    throw new Error(`Error fetching open food ${openFoodId}`)
}

export async function importOpenFood(openFoodId: number): Promise<FoodWithPortion> {
    const url = new URL(`${BACKEND_URL}/foods/open-food/${openFoodId}/import`)
    const response = await backendFetch(url.toString(), { method: "POST" })

    if (response.ok) {
        return await response.json() as FoodWithPortion
    }

    throw new Error(`Error importing open food ${openFoodId}`)
}

export async function trackOpenFood(openFoodId: number, trackedFood: Omit<CreateTrackedFood, "foodId">) {
    const url = new URL(`${BACKEND_URL}/foods/open-food/${openFoodId}/track`)
    const response = await backendFetch(url.toString(), {
        method: "POST",
        body: JSON.stringify(trackedFood), headers: {
            'Content-Type': 'application/json'
        }
    })
    if (response.ok) {
        revalidatePath("/foods/track")
        return
    }
    throw new Error(`Error tracking open food ${openFoodId}`)
}

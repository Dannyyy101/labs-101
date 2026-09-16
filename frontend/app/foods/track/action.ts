'use server'

import { auth } from "@/lib/auth"
import { BACKEND_URL } from "@/utils/constants"
import { ApiError } from "@/utils/types/api"
import { CreateFoodWithAmount, Food, FoodWithAmount, FoodWithLastEntry, SearchFood, TrackFoodForUser } from "@/utils/types/food"
import { Page } from "@/utils/types/page"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"
import { headers } from "next/headers"

export async function findFoodByNameAndUserId(name: string, filter?: { page?: number }): Promise<Page<SearchFood>> {
    const url = new URL(BACKEND_URL + "/foods/search/byNameAndUser")

    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")

    url.searchParams.append("name", name)
    url.searchParams.append("userId", session.user.id)
    if (filter?.page) {
        url.searchParams.append("page", String(filter.page))
    }


    const response = await fetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Page<SearchFood>
    }

    throw new Error("Error fetching foods")
}

export async function getTrackedFood(date: Date): Promise<FoodWithAmount[]> {
    const session = await auth.api.getSession({
        headers: await headers()
    })

    if (!session) throw new Error("User is currently not in a session")

    const url = new URL(`${BACKEND_URL}/users/${session.user.id}/tracked-food`)

    url.searchParams.append("date", date.toISOString())

    const response = await fetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as FoodWithAmount[]
    }

    throw new Error("Error fetching tracked foods")
}

export async function getTrackedFoodByFoodId(foodId: number): Promise<FoodWithLastEntry> {
    const session = await auth.api.getSession({
        headers: await headers()
    })

    if (!session) throw new Error("User is currently not in a session")

    const url = new URL(`${BACKEND_URL}/users/${session.user.id}/foods/${foodId}/last`)

    const response = await fetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as FoodWithLastEntry
    }

    throw new Error(`Error fetching tracked food by ${foodId}`)
}

export async function createTrackFood(foodWithAmount: CreateFoodWithAmount) {
    const url = new URL(`${BACKEND_URL}/foods/${foodWithAmount.foodId}/track`)

    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")
    console.log(url)
    const response = await fetch(url.toString(), {
        method: "POST",
        body: JSON.stringify({ ...foodWithAmount, userId: session.user.id }), headers: {
            'Content-Type': 'application/json'
        }
    })
    if (response.ok) {
        revalidatePath("/foods/track")
        return
    }
    throw new Error(await response.json())
}

export async function findAndSafeFoodIfNotExistByBarcode(code: string): Promise<Result<Food>> {
    const url = new URL(`${BACKEND_URL}/foods/bar-code/${code}`)
    const response = await fetch(url.toString())

    if (response.ok) {
        return { ok: true, data: await response.json() as Food }
    }

    const apiError = await response.json().catch(() => null) as ApiError | null
    return {
        ok: false,
        error: apiError?.errorMessage ?? ""
    }
}
'use server'

import { auth } from "@/lib/auth"
import { BACKEND_URL } from "@/utils/constants"
import { Food, FoodWithAmount, TrackFoodForUser } from "@/utils/types/food"
import { Page } from "@/utils/types/page"
import { revalidatePath } from "next/cache"
import { headers } from "next/headers"

export async function findFoodByNameAndUserId(name: string, filter?: { page?: number }): Promise<Page<Food>> {
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
        return await response.json() as Page<Food>
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

export async function createTrackFood(foodWithAmount: FoodWithAmount) {
    const url = new URL(`${BACKEND_URL}/foods/${foodWithAmount.id}/track`)

    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")

    await fetch(url.toString(), {
        method: "POST",
        body: JSON.stringify({ foodId: foodWithAmount.id, userId: session.user.id, amount: foodWithAmount.amount, meal: foodWithAmount.meal } as TrackFoodForUser), headers: {
            'Content-Type': 'application/json'
        }
    })
    revalidatePath("/foods/track")
}
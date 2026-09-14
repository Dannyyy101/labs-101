'use server'

import { BACKEND_URL } from "@/utils/constants";
import { FoodWithPortion } from "@/utils/types/food";
import { Page } from "@/utils/types/page";
import { revalidatePath } from "next/cache";

export async function getAllFoods(page: number = 0, sort: string = "asc", query: string = ""): Promise<Page<FoodWithPortion>> {
    const url = new URL(BACKEND_URL + "/foods")

    url.searchParams.append("page", String(page))
    url.searchParams.append("sort", String(sort))
    url.searchParams.append("query", String(query))

    const response = await fetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Page<FoodWithPortion>
    }

    throw new Error("Error fetching foods")
}

export async function createFood(food: Omit<FoodWithPortion, "id">) {

}

export async function updateFood(food: FoodWithPortion) {
    console.log(food)
    const response = await fetch(BACKEND_URL + "/foods/" + food.id, {
        method: "PUT", body: JSON.stringify(food), headers: {
            'Content-Type': 'application/json'
        }
    })

    if (response.ok) {
        revalidatePath("/foods")
        return
    }

    throw new Error("Error updating foods")
}

export async function deleteFood(foodId: number) {

}
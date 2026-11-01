'use server'

import { api } from "@/lib/api/client";
import { Food, FoodWithPortion } from "@/utils/types/food";
import { Page } from "@/utils/types/page";
import { revalidatePath } from "next/cache";

// without portions, EditFood starts with none
export async function getAllFoods(page: number = 0, sort: string = "asc", query: string = ""): Promise<Page<Food>> {
    const { data } = await api.GET("/api/foods", { params: { query: { page, sort: [sort], query } } })

    if (!data) throw new Error("Error fetching foods")
    return data
}

export async function createFood(food: Omit<FoodWithPortion, "id">) {
    const { response } = await api.POST("/api/foods", { body: food })

    if (!response.ok) return
    revalidatePath("/foods")
}

export async function updateFood(food: FoodWithPortion) {
    const { response } = await api.PUT("/api/foods/{id}", { params: { path: { id: food.id } }, body: food })

    if (!response.ok) throw new Error("Error updating foods")
    revalidatePath("/foods")
}

export async function deleteFood(foodId: number) {

}

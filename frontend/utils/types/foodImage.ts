import { FoodWithPortion, Meal } from "./food"

// PENDING until the job analyzed the photo, READY until the user accepted or rejected it
export type FoodImageStatus = "PENDING" | "READY" | "FAILED" | "ACCEPTED" | "REJECTED"

export interface FoodImageItem {
    // name the model gave the food
    query: string
    amount: number
    // "g", "ml" or a portion like "Stück"
    unit: string
    // estimated by the model, 1 for g and ml
    gramsPerUnit: number
    // id is null for open foods, they are only copied into our foods once tracked
    food: (Omit<FoodWithPortion, "id"> & { id: number | null }) | null
    openFoodId: number | null
}

export interface FoodImageAnalysis {
    id: number
    status: FoodImageStatus
    meal: Meal
    // what the user said is on the photo, passed to the model as hint
    description: string | null
    items: FoodImageItem[]
    // only set when the analysis failed
    error: string | null
    createdAt: string
    analyzedAt: string | null
    reviewedAt: string | null
}

// failed photos don't count
export interface FoodImageUsage {
    used: number
    limit: number
}

export interface AcceptFoodImage {
    meal: Meal
    // a unit the food doesn't have yet is added as portion with gramsPerUnit
    items: { foodId: number | null, openFoodId: number | null, amount: number, unit: string, gramsPerUnit: number }[]
}

// served by the route handler next to the review page, it adds the access token
export const foodImageUrl = (id: number) => `/foods/images/${id}/image`

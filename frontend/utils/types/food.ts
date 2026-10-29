import type { Schemas } from "@/lib/api/types"

export type Food = Schemas["FoodDto"]
export type FoodPortion = Schemas["FoodPortionDto"]

// a food of our database, the ones of the open food database have no id yet
export type FoodWithPortion = Omit<Schemas["FoodWithPortionsDto"], "id"> & { id: number }

// null while the food only exists in the open food database
export type SearchFood = Schemas["SearchFoodResponseDto"]

export type TrackedFood = Schemas["TrackedFoodDto"]

export interface CreateTrackedFood {
    id?: number | null;
    foodId: number
    amount: number,
    meal: Meal
    portionId: number | null
}

export type FoodWithLastEntry = Schemas["FoodWithLastEntryAndPortionsDto"]

export type Meal = Schemas["FoodImageAnalysisDto"]["meal"]
// fails to compile when the backend gets another meal
export const Meal = {
    BREAKFAST: "BREAKFAST",
    LUNCH: "LUNCH",
    DINNER: "DINNER",
    SNACK: "SNACK",
} as const satisfies { [M in Meal]: M }

// food.id is null for open foods, they are only copied into our foods once tracked
export type ExtractedFood = Schemas["ExtractedFoodDto"]

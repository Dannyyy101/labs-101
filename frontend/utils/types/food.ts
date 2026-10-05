
export interface Food {
    id: number,
    blsCode: string,
    name: string,
    kcal: number | null,
    water: number | null,
    protein: number | null,
    fat: number | null,
    carbohydrates: number | null
    fiber: number | null
}

export interface FoodWithPortion extends Food {
    portions: FoodPortion[],
}

export interface SearchFood {
    // null while the food only exists in the open food database
    id: number | null,
    name: string,
    openFoodId: number | null
}

export interface CreateFoodWithAmount {
    foodId: number,
    userId: string,
    amount: number,
    portionId: number | null
    meal: Meal
}

export interface TrackedFood {
    id: number
    food: Food
    amount: number,
    meal: { type: string, typeLabel: string }
    portion: FoodPortion | null
    createDate: string
}

export interface CreateTrackedFood {
    id?: number | null;
    foodId: number
    amount: number,
    meal: Meal
    portionId: number | null
}

export interface FoodPortion {
    id: number,
    grams: number,
    label: string,
    isDefault: boolean
}

export interface FoodWithLastEntry extends Food {
    portions: FoodPortion[]
    lastEntry?: {
        amount: number,
    }
}

export interface TrackFoodForUser {
    foodId: number,
    userId: string
    amount: number
}

export enum Meal {
    BREAKFAST = "BREAKFAST",
    LUNCH = "LUNCH",
    DINNER = "DINNER",
    SNACK = "SNACK"
}

export interface ExtractedFood {
    query: string,
    amount: number,
    unit: string | null,
    // id is null for open foods, they are only copied into our foods once tracked
    food: (Omit<FoodWithPortion, "id"> & { id: number | null }) | null
    openFoodId: number | null
}

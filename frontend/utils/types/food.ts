
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
    portions: FoodPortion[]

}

export interface SearchFood {
    id: number,
    name: string
}

export interface CreateFoodWithAmount {
    foodId: number,
    userId: string,
    amount: number,
    portionId: number | null
    meal: string
}

export interface FoodWithAmount extends Food {
    amount: number,
    meal: { type: string, typeLabel: string }
    portion: FoodPortion | null
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
        meal: string
    }
}

export interface TrackFoodForUser {
    foodId: number,
    userId: string
    amount: number
}
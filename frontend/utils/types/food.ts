
export interface Food {
    id: number,
    blsCode: string,
    name: string,
    kcal: number,
    water: number,
    protein: number,
    fat: number,
    carbohydrates: number
    fiber: number
}

export interface FoodWithAmount extends Food {
    amount: number,
    meal: string
}

export interface TrackFoodForUser {
    foodId: number,
    userId: string
    amount: number
}
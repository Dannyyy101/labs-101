import { FoodWithAmount } from "./types/food";

type NumberKeys<T> = {
    [K in keyof T]-?: NonNullable<T[K]> extends number ? K : never
}[keyof T];

export function getNutritionForAmount(
    food: FoodWithAmount,
    nutrition: Exclude<NumberKeys<FoodWithAmount>, "amount" | "id">
): number {
    const per100g = food[nutrition] ?? 0;
    const grams = food.amount * (food.portion?.grams ?? 1);
    return (per100g * grams) / 100;
}
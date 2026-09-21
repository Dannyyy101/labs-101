import { Food, TrackedFood } from "./types/food";

type NumberKeys<T> = {
    [K in keyof T]-?: NonNullable<T[K]> extends number ? K : never
}[keyof T];

export function getNutritionForAmount(
    food: Omit<TrackedFood, 'id' | 'meal'>,
    nutrition: Exclude<NumberKeys<Food>, "id">
): number {
    const per100g = food.food[nutrition] ?? 0;
    const grams = food.amount * (food.portion?.grams ?? 1);
    return Math.round((per100g * grams) / 100 * 10) / 10;
}
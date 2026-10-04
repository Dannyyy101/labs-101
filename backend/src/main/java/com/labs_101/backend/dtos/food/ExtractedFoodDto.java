package com.labs_101.backend.dtos.food;

/**
 * @param unit       the unit as written in the text (e.g. "Tasse"), null if
 *                   none was given
 * @param openFoodId set if the match only exists in the open food database. In
 *                   that case {@code food.id} is null and the food gets copied
 *                   into our foods once it is tracked.
 */
public record ExtractedFoodDto(String query, Double amount, String unit, FoodWithPortionsDto food,
        Long openFoodId) {
}

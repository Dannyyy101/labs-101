package com.labs_101.backend.dtos.foodImage;

import com.labs_101.backend.dtos.food.FoodWithPortionsDto;

/**
 * A food recognized on the photo, like {@link com.labs_101.backend.dtos.food.ExtractedFoodDto}
 * with the grams per unit the model estimated.
 *
 * @param food       best match, null if there is none
 * @param openFoodId set if the match only exists in the open food database
 */
public record FoodImageItemDto(String query, Double amount, String unit, Double gramsPerUnit,
        FoodWithPortionsDto food, Long openFoodId) {
}

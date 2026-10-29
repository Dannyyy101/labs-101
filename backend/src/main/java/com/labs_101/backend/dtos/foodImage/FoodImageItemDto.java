package com.labs_101.backend.dtos.foodImage;

import org.jspecify.annotations.Nullable;

import com.labs_101.backend.dtos.food.FoodWithPortionsDto;

/**
 * A food recognized on the photo, like {@link com.labs_101.backend.dtos.food.ExtractedFoodDto}
 * with the grams per unit the model estimated.
 *
 * @param food       best match, null if there is none
 * @param openFoodId set if the match only exists in the open food database
 */
public record FoodImageItemDto(String query, Double amount, @Nullable String unit, @Nullable Double gramsPerUnit,
        @Nullable FoodWithPortionsDto food, @Nullable Long openFoodId) {
}

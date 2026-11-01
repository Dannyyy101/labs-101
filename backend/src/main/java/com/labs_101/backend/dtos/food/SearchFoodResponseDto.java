package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

/**
 * @param openFoodId set if the food only exists in the open food database. In
 *                   that case {@code id} is null and the food has to be
 *                   imported before it can be tracked.
 */
public record SearchFoodResponseDto(@Nullable Long id, String name, @Nullable Long openFoodId) {

}

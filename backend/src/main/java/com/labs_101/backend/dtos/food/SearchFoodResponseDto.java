package com.labs_101.backend.dtos.food;

/**
 * @param openFoodId set if the food only exists in the open food database. In
 *                   that case {@code id} is null and the food has to be
 *                   imported before it can be tracked.
 */
public record SearchFoodResponseDto(Long id, String name, Long openFoodId) {

}

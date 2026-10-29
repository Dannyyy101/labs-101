package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

public record CreateFoodUserDto(Long foodId, @Nullable String userId, Double amount, String meal, @Nullable Long portionId) {

}

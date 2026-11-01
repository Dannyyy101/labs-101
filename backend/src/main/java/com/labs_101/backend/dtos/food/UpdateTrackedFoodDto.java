package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

public record UpdateTrackedFoodDto(@Nullable Long foodId, Double amount, String meal, @Nullable Long portionId) {

}

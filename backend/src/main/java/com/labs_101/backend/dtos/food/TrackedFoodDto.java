package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record TrackedFoodDto(Long id,
                FoodDto food, Double amount, MealDto meal, @Nullable FoodPortionDto portion, Instant createDate) {
}
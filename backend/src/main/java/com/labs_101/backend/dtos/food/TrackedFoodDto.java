package com.labs_101.backend.dtos.food;

import java.time.Instant;

public record TrackedFoodDto(Long id,
                FoodDto food, Double amount, MealDto meal, FoodPortionDto portion, Instant createDate) {
}
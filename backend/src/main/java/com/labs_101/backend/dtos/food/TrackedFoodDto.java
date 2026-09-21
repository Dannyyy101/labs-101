package com.labs_101.backend.dtos.food;

public record TrackedFoodDto(Long id,
                FoodDto food, Double amount, MealDto meal, FoodPortionDto portion) {
}
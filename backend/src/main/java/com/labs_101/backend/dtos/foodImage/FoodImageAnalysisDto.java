package com.labs_101.backend.dtos.foodImage;

import java.time.Instant;
import java.util.List;

import com.labs_101.backend.entities.food.FoodImageStatus;
import com.labs_101.backend.entities.food.MealType;

/** The photo itself is at {@code /api/users/me/food-images/{id}/image}. */
public record FoodImageAnalysisDto(Long id, FoodImageStatus status, MealType meal, String description,
        List<FoodImageItemDto> items,
        String error, Instant createdAt, Instant analyzedAt, Instant reviewedAt) {
}

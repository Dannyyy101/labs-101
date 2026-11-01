package com.labs_101.backend.dtos.foodImage;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

import com.labs_101.backend.entities.food.FoodImageStatus;
import com.labs_101.backend.entities.food.MealType;

/** The photo itself is at {@code /api/users/me/food-images/{id}/image}. */
public record FoodImageAnalysisDto(Long id, FoodImageStatus status, MealType meal, @Nullable String description,
        List<FoodImageItemDto> items,
        @Nullable String error, Instant createdAt, @Nullable Instant analyzedAt, @Nullable Instant reviewedAt) {
}

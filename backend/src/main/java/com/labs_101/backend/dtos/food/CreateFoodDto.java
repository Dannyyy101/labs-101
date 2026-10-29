package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

public record CreateFoodDto(
        @Nullable String blsCode,
        String name,
        @Nullable Double kcal,
        @Nullable Double water,
        @Nullable Double protein,
        @Nullable Double fat,
        @Nullable Double carbohydrates,
        @Nullable Double fiber) {
}
package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record UpdateFoodDto(@Nullable Long id, @Nullable String blsCode,
        String name,
        @Nullable Double kcal,
        @Nullable Double water,
        @Nullable Double protein,
        @Nullable Double fat,
        @Nullable Double carbohydrates,
        @Nullable Double fiber,
        @Nullable List<UpdateFoodPortionDto> portions) {

}

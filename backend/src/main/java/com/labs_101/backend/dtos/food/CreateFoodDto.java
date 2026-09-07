package com.labs_101.backend.dtos.food;

public record CreateFoodDto(
        String blsCode,
        String name,
        Double kcal,
        Double water,
        Double protein,
        Double fat,
        Double carbohydrates,
        Double fiber) {
}
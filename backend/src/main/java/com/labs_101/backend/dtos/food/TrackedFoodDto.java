package com.labs_101.backend.dtos.food;

import java.util.ArrayList;
import java.util.List;

public record TrackedFoodDto(Long id, String blsCode, String name,
        Double kcal,
        Double water,
        Double protein,
        Double fat,
        Double carbohydrates,
        Double fiber,
        LastTrackedFoodEntryDto lastEntry,
        List<FoodPortionDto> portions) {
    public record LastTrackedFoodEntryDto(Double amount, MealDto meal) {

    }

}
package com.labs_101.backend.dtos.food;

public record FoodUserDto(Long id, String blsCode,
                String name,
                Double kcal,
                Double water,
                Double protein,
                Double fat,
                Double carbohydrates,
                Double fiber, String userId, Double amount,
                MealDto meal,
                FoodPortionDto portion) {

}

package com.labs_101.backend.dtos.food;

public record CreateOpenFoodDto(String barCode,
        String name,
        Double kcal,
        Double water,
        Double protein,
        Double fat,
        Double carbohydrates,
        Double fiber,
        String company) {

}

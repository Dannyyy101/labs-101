package com.labs_101.backend.dtos.food;

import java.util.List;

public record FoodWithPortionsDto(Long id, String blsCode,
        String name,
        Double kcal,
        Double water,
        Double protein,
        Double fat,
        Double carbohydrates,
        Double fiber,
        List<FoodPortionDto> portions) {

}

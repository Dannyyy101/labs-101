package com.labs_101.backend.dtos.food;

public record CreateFoodUserDto(Long foodId, Long openFoodId, String userId, Double amount, String meal,
        Long portionId) {

}

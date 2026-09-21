package com.labs_101.backend.dtos.food;

public record UpdateTrackedFoodDto(Long foodId, Double amount, String meal, Long portionId) {

}

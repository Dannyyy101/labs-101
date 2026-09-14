package com.labs_101.backend.dtos.food;

public record UpdateFoodPortionDto(Long id, Double grams, String label, Boolean isDefault) {

}

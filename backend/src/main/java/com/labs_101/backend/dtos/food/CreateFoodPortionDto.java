package com.labs_101.backend.dtos.food;

public record CreateFoodPortionDto(String label, Double grams, boolean isDefault) {
}
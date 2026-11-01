package com.labs_101.backend.dtos.food;

import org.jspecify.annotations.Nullable;

public record UpdateFoodPortionDto(@Nullable Long id, Double grams, String label, @Nullable Boolean isDefault) {

}

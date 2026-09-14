package com.labs_101.backend.entities.food;

public enum MealType {
    BREAKFAST, LUNCH, DINNER, SNACK;

    public String messageKey() {
        return "meal.type." + name().toLowerCase();
    }
}
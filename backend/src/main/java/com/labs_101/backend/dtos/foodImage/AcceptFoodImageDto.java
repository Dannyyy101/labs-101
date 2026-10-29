package com.labs_101.backend.dtos.foodImage;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The foods as checked by the user, they get tracked for {@code meal}. A unit
 * the food doesn't have yet is added as portion with {@code gramsPerUnit}.
 */
public record AcceptFoodImageDto(String meal, List<Item> items) {

    /** {@code foodId} or {@code openFoodId} is required */
    public record Item(@Nullable Long foodId, @Nullable Long openFoodId, Double amount, @Nullable String unit, @Nullable Double gramsPerUnit) {
    }
}

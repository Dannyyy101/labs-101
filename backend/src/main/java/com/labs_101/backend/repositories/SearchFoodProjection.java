package com.labs_101.backend.repositories;

/**
 * A food or open food found by {@link FoodRepository#search}.
 */
public interface SearchFoodProjection {
    Long getId();

    String getName();

    /** true if {@link #getId()} is the id of an open food */
    Boolean getOpenFood();
}

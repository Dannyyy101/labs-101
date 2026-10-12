package com.labs_101.backend.dtos.profile;

import java.time.LocalDate;

/**
 * @param meals       distinct meals (breakfast, lunch, …) with at least one tracked food
 * @param runDistance meters
 * @param workoutMinutes finished workout sessions
 */
public record ActivityDayDto(LocalDate date, int meals, int runs, double runDistance, int workouts,
        double workoutMinutes, boolean loggedIn) {
}

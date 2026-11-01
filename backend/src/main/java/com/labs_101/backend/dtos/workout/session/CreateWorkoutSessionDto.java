package com.labs_101.backend.dtos.workout.session;

import org.jspecify.annotations.Nullable;

/**
 * Starts a workout, with the exercises of the template or without any
 * exercises when {@code workoutId} is null.
 */
public record CreateWorkoutSessionDto(@Nullable Long workoutId, @Nullable String name) {
}

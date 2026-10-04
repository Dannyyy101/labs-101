package com.labs_101.backend.dtos.workout.session;

/**
 * Starts a workout, with the exercises of the template or without any
 * exercises when {@code workoutId} is null.
 */
public record CreateWorkoutSessionDto(Long workoutId, String name) {
}

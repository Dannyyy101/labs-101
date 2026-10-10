package com.labs_101.backend.dtos.workout.session;

/** @param restSeconds the rest after the set, null for the default */
public record SessionSetDto(int reps, double weightKg, Integer rpe, boolean done, Integer restSeconds) {
}

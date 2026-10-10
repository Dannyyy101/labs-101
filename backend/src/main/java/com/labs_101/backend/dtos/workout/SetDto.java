package com.labs_101.backend.dtos.workout;

/** @param restSeconds the rest after the set, null for the default */
public record SetDto(int order, int reps, double weightKg, Integer rpe, Integer restSeconds) {
}

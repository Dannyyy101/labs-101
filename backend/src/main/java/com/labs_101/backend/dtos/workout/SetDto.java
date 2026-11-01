package com.labs_101.backend.dtos.workout;

import org.jspecify.annotations.Nullable;

/** @param restSeconds the rest after the set, null for the default */
public record SetDto(int order, int reps, double weightKg, @Nullable Integer rpe, @Nullable Integer restSeconds) {
}

package com.labs_101.backend.dtos.workout.session;

import org.jspecify.annotations.Nullable;

/** @param restSeconds the rest after the set, null for the default */
public record SessionSetDto(int reps, double weightKg, @Nullable Integer rpe, boolean done, @Nullable Integer restSeconds) {
}

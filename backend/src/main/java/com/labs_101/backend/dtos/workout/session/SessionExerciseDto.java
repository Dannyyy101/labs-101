package com.labs_101.backend.dtos.workout.session;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @param note   a short note of the user, e.g. the seat height, null when there is none
 * @param cardio minutes of cardio like running or rowing, null for a strength exercise with sets
 */
public record SessionExerciseDto(@Nullable Long exerciseId, String name, @Nullable String note, List<SessionSetDto> sets,
        @Nullable SessionCardioDto cardio) {
}

package com.labs_101.backend.dtos.workout.session;

import java.util.List;

/**
 * @param note   a short note of the user, e.g. the seat height, null when there is none
 * @param cardio minutes of cardio like running or rowing, null for a strength exercise with sets
 */
public record SessionExerciseDto(Long exerciseId, String name, String note, List<SessionSetDto> sets,
        SessionCardioDto cardio) {
}

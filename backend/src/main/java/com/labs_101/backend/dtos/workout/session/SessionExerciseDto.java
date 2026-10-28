package com.labs_101.backend.dtos.workout.session;

import java.util.List;

/** @param note a short note of the user, e.g. the seat height, null when there is none */
public record SessionExerciseDto(Long exerciseId, String name, String note, List<SessionSetDto> sets) {
}

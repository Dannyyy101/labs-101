package com.labs_101.backend.dtos.workout.session;

import java.util.List;

public record SessionExerciseDto(Long exerciseId, String name, List<SessionSetDto> sets) {
}

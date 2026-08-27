package com.labs_101.backend.dtos.workout;

import java.util.List;

public record StrengthWorkoutItemDto(String exerciseId, int order, String type, List<TrainingSetDto> sets)
        implements CreateWorkoutExerciseDto {
}

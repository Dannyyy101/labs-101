package com.labs_101.backend.dtos.workout;

import java.util.List;

import com.labs_101.backend.dtos.exercises.ExerciseDto;

public record StrengthItemDto(ExerciseDto exercise, List<SetDto> sets) implements ExerciseItem {
}

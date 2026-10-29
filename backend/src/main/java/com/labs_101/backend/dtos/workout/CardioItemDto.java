package com.labs_101.backend.dtos.workout;

import java.util.List;

import com.labs_101.backend.dtos.exercises.ExerciseDto;

/**
 * Cardio in a template, only with its minutes.
 *
 * @param sets always empty, older apps expect every exercise to have sets
 */
public record CardioItemDto(ExerciseDto exercise, int minutes, List<SetDto> sets) implements ExerciseItem {
    public CardioItemDto {
        sets = List.of();
    }
}

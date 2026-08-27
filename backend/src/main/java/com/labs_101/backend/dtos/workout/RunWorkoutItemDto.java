package com.labs_101.backend.dtos.workout;

public record RunWorkoutItemDto(String exerciseId, int order, String type) implements CreateWorkoutExerciseDto {
}

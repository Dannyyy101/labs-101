package com.labs_101.backend.dtos.workout;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public record CreateWorkoutDto(
        String name,
        @Nullable Instant scheduledFor,
        List<ExerciseItem> exercises) {
}

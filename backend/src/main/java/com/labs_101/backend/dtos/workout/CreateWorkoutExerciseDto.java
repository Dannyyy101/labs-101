package com.labs_101.backend.dtos.workout;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StrengthWorkoutItemDto.class, name = "strength-training"),
        @JsonSubTypes.Type(value = RunWorkoutItemDto.class, name = "run-training")
})
public sealed interface CreateWorkoutExerciseDto permits StrengthWorkoutItemDto, RunWorkoutItemDto {
    String exerciseId();

    int order();

    String type();
}
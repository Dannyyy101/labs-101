package com.labs_101.backend.dtos.workout;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.labs_101.backend.dtos.exercises.ExerciseDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StrengthItemDto.class, name = "STRENGTH_EXERCISE"),
})
public sealed interface ExerciseItem permits StrengthItemDto {
    ExerciseDto exercise();
}
package com.labs_101.backend.dtos.workoutTemplate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StrengthItemDto.class, name = "strength-training"),
        @JsonSubTypes.Type(value = RunItemDto.class, name = "run-training")
})
public sealed interface PlanItemDto permits StrengthItemDto, RunItemDto {
    String exerciseId();

    int order();

    String type();
}
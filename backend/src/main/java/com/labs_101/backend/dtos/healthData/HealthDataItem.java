package com.labs_101.backend.dtos.healthData;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StepsDto.class, name = "STEPS"),
})
public sealed interface HealthDataItem permits StepsDto {
    String type();
}
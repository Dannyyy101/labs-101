package com.labs_101.backend.dtos.workoutTemplate;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateWorkoutTemplateDto {
    private String name;
    private List<PlanItemDto> items;
}





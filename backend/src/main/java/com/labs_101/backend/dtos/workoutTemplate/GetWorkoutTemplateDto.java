package com.labs_101.backend.dtos.workoutTemplate;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GetWorkoutTemplateDto {
    private String id;
    private String type;
    private String name;
}
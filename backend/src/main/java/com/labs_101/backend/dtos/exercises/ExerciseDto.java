package com.labs_101.backend.dtos.exercises;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ExerciseDto {
    private String id;
    private String name;
    private String description;
}

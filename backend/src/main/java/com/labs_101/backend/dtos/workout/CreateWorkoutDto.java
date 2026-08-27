package com.labs_101.backend.dtos.workout;

import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateWorkoutDto {
    private String name;
    private List<CreateWorkoutExerciseDto> items;
}

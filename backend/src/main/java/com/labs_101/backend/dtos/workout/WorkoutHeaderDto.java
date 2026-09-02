package com.labs_101.backend.dtos.workout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutHeaderDto {
    private Long id;
    private String name;
}

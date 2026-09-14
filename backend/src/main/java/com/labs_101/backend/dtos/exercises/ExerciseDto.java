package com.labs_101.backend.dtos.exercises;

import java.util.ArrayList;

import com.labs_101.backend.dtos.BodyPartDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ExerciseDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private ArrayList<BodyPartDto> bodyParts;
}

package com.labs_101.backend.dtos.exercises;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import com.labs_101.backend.dtos.BodyPartDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateExerciseDto {
    private @Nullable String slug;
    private String name;
    private @Nullable String description;
    private String type;
    private ArrayList<BodyPartDto> bodyParts;
}

package com.labs_101.backend.mapper;

import java.util.ArrayList;

import com.labs_101.backend.dtos.BodyPartDto;
import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.entities.BodyPart;
import com.labs_101.backend.entities.Exercise;

public class ExerciseMapper {
        public static Exercise fromCreateExerciseDto(CreateExerciseDto dto) {
                ArrayList<BodyPart> bodyParts = new ArrayList<>(dto.getBodyParts().stream()
                                .map((part) -> new BodyPart(null, part.getSlug(), part.getColor(), part.getIntensity(),
                                                part.getSide(),
                                                null))
                                .toList());
                return new Exercise(null, dto.getName(), dto.getDescription(), dto.getType(),
                                bodyParts);
        }

        public static ExerciseDto fromExercise(Exercise exercise) {
                ArrayList<BodyPartDto> bodyParts = new ArrayList<>(exercise.getBodyParts().stream()
                                .map((part) -> new BodyPartDto(part.getId(), part.getSlug(), part.getColor(),
                                                part.getIntensity(),
                                                part.getSide()))
                                .toList());
                return new ExerciseDto(exercise.getId(), exercise.getName(), exercise.getDescription(),
                                exercise.getType(),
                                bodyParts);
        }
}

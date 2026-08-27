package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.entities.Exercise;

public class ExerciseMapper {
    public static Exercise fromCreateExerciseDto(CreateExerciseDto dto){
        return new Exercise(null, dto.getName(), dto.getDescription());
    }

    public static ExerciseDto fromExercise(Exercise exercise){
        return new ExerciseDto(exercise.getId(), exercise.getName(), exercise.getDescription());
    }
}

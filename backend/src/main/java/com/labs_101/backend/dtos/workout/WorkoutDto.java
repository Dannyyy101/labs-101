package com.labs_101.backend.dtos.workout;

import java.util.ArrayList;
import java.util.List;

import com.labs_101.backend.entities.workout.WorkoutExercise;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkoutDto extends WorkoutHeaderDto {
    private List<ExerciseItem> workoutExercises = new ArrayList<>();

    public WorkoutDto(Long id, String name, List<ExerciseItem> exercises) {
        super(id, name);
        this.workoutExercises = exercises;
    }
}

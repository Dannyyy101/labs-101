package com.labs_101.backend.dtos.workout;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkoutDto extends WorkoutHeaderDto {
    private List<ExerciseItem> workoutExercises = new ArrayList<>();
    /** Name of the creator, null if unknown. */
    private String ownerName;
    /** Whether the requesting user may change and delete the template. */
    private boolean editable;

    public WorkoutDto(Long id, String name, List<ExerciseItem> exercises, String ownerName, boolean editable) {
        super(id, name);
        this.workoutExercises = exercises;
        this.ownerName = ownerName;
        this.editable = editable;
    }
}

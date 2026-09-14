package com.labs_101.backend.entities.workout.workoutExercises;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import java.util.List;

import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.ExerciseType;
import com.labs_101.backend.entities.workout.WorkoutExercise;
import com.labs_101.backend.entities.workout.WorkoutExercise.IExercise;

@Entity
@NoArgsConstructor
@DiscriminatorValue("0")
public class StrengthItem extends WorkoutExercise<StrengthItem.Settings> {

    public StrengthItem(Exercise exercise, Settings settings) {
        setExercise(exercise);
        setType(ExerciseType.STRENGTH_EXERCISE);
        setSettings(settings);
    }

    public record Settings(List<Set> sets) implements IExercise {
    }

    public record Set(int order, int reps, double weightKg, Integer rpe) {
    }
}
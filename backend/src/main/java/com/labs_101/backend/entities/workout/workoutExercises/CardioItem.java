package com.labs_101.backend.entities.workout.workoutExercises;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.ExerciseType;
import com.labs_101.backend.entities.workout.WorkoutExercise;
import com.labs_101.backend.entities.workout.WorkoutExercise.IExercise;

/** Cardio before, between or after the strength exercises, e.g. running or the rowing machine, only its minutes are planned. */
@Entity
@NoArgsConstructor
@DiscriminatorValue("1")
public class CardioItem extends WorkoutExercise<CardioItem.Settings> {

    public CardioItem(Exercise exercise, Settings settings) {
        setExercise(exercise);
        setType(ExerciseType.CARDIO_EXERCISE);
        setSettings(settings);
    }

    public record Settings(int minutes) implements IExercise {
    }
}

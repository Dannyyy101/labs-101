package com.labs_101.backend.entities.workout;

import com.labs_101.backend.entities.Exercise;

public sealed interface WorkoutExercise permits StrengthWorkoutItem {
    Exercise exercise();

    int order();

    String type();
}

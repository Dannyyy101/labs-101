package com.labs_101.backend.entities.workout;

import java.util.List;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.labs_101.backend.entities.Exercise;

@Document
@TypeAlias("strength")
public record StrengthWorkoutItem(@DBRef Exercise exercise, int order, String type, List<TrainingSet> sets)
        implements WorkoutExercise {
    @PersistenceCreator
    public StrengthWorkoutItem {
    }
}

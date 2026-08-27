package com.labs_101.backend.entities.workout;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Workout {
    private @Id String id;
    private String name;
    private List<WorkoutExercise> items;

    public Workout(String id) {
        this.id = id;
    }
}

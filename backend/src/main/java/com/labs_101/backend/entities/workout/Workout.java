package com.labs_101.backend.entities.workout;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout")
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double duration;

    @OneToMany(mappedBy = "workout", targetEntity = WorkoutExercise.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutExercise<?>> exercises = new ArrayList<>();

    public void addExercise(WorkoutExercise<?> exercise) {
        exercise.setPosition(exercises.size());
        exercise.setWorkout(this);
        exercises.add(exercise);
    }

    public void removeExercise(WorkoutExercise<?> exercise) {
        exercises.remove(exercise);
        exercise.setWorkout(null);
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).setPosition(i);
        }
    }
}

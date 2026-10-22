package com.labs_101.backend.entities.workout;

import java.util.ArrayList;
import java.util.List;

import com.labs_101.backend.entities.User;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout")
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double duration;

    /** Who created the template, null for templates from before the creator was stored. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "workout", targetEntity = WorkoutExercise.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutExercise<?>> exercises = new ArrayList<>();

    /** Everyone can use a template, only its creator can change or delete it (anyone, if it has none). */
    public boolean isEditableBy(String userId) {
        return owner == null || owner.getId().equals(userId);
    }

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

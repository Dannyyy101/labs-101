package com.labs_101.backend.entities.workout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Friends the creator shared the template with, they can train it but not change it. */
    @ManyToMany
    @JoinTable(name = "workout_share",
            joinColumns = @JoinColumn(name = "workout_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> sharedWith = new HashSet<>();

    @OneToMany(mappedBy = "workout", targetEntity = WorkoutExercise.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutExercise<?>> exercises = new ArrayList<>();

    /**
     * Only its creator and the friends it was shared with see a template. Templates from before the
     * creator was stored stay visible to everyone, no one knows whose they are.
     */
    public boolean isVisibleTo(String userId) {
        return isEditableBy(userId) || isSharedWith(userId);
    }

    /** Only its creator can change or delete a template (anyone, if it has none). */
    public boolean isEditableBy(String userId) {
        return owner == null || owner.getId().equals(userId);
    }

    public boolean isSharedWith(String userId) {
        return sharedWith.stream().anyMatch((user) -> user.getId().equals(userId));
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

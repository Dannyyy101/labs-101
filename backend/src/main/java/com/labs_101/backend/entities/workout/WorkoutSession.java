package com.labs_101.backend.entities.workout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.labs_101.backend.entities.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A workout a user did (or is doing right now, while {@code endedAt} is null).
 * The exercises are a copy of the template it was started from, so changing
 * or deleting the template later doesn't change the history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_session", indexes = @Index(columnList = "user_id, started_at"))
public class WorkoutSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The template it was started from, null for a free workout or a deleted template. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    private String name;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "log", nullable = false, columnDefinition = "jsonb")
    private Log log = new Log(new ArrayList<>());

    public record Log(List<Entry> exercises) {
    }

    /** {@code name} is kept so the history still reads right after the exercise is renamed or deleted. */
    public record Entry(Long exerciseId, String name, List<Set> sets) {
    }

    /** {@code done} is checked off during the workout, only done sets count. */
    public record Set(int reps, double weightKg, Integer rpe, boolean done) {
    }
}

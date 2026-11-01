package com.labs_101.backend.dtos.workout.session;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * A workout of the user, {@code endedAt} is null while it is still running.
 *
 * @param workoutId the template it was started from, null for a free workout
 */
public record WorkoutSessionDto(
        Long id,
        @Nullable Long workoutId,
        String name,
        Instant startedAt,
        @Nullable Instant endedAt,
        List<SessionExerciseDto> exercises) {
}

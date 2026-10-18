package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.workout.WorkoutSession;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    Optional<WorkoutSession> findByIdAndUser_Id(Long id, String userId);

    Optional<WorkoutSession> findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(String userId);

    /** Running sessions of the users started after {@code after}, newest first. */
    List<WorkoutSession> findByUser_IdInAndEndedAtIsNullAndStartedAtAfterOrderByStartedAtDesc(
            Collection<String> userIds, Instant after);

    /** Finished sessions started in the range, newest first. */
    List<WorkoutSession> findByUser_IdAndEndedAtIsNotNullAndStartedAtBetweenOrderByStartedAtDesc(String userId,
            Instant from, Instant to);

    List<WorkoutSession> findByWorkout_Id(Long workoutId);
}

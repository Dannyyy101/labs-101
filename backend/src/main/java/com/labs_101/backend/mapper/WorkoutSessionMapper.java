package com.labs_101.backend.mapper;

import java.util.List;

import com.labs_101.backend.dtos.workout.session.SessionCardioDto;
import com.labs_101.backend.dtos.workout.session.SessionExerciseDto;
import com.labs_101.backend.dtos.workout.session.SessionSetDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.workout.WorkoutSession;

public class WorkoutSessionMapper {
    public static final int NOTE_LENGTH = 500;

    public static WorkoutSessionDto toDto(WorkoutSession session) {
        return new WorkoutSessionDto(
                session.getId(),
                session.getWorkout() != null ? session.getWorkout().getId() : null,
                session.getName(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getLog().exercises().stream()
                        .map((entry) -> new SessionExerciseDto(entry.exerciseId(), entry.name(), entry.note(),
                                entry.sets().stream()
                                        .map((set) -> new SessionSetDto(set.reps(), set.weightKg(), set.rpe(), set.done(), set.restSeconds()))
                                        .toList(),
                                entry.cardio() == null ? null : new SessionCardioDto(entry.cardio().minutes(), entry.cardio().done())))
                        .toList());
    }

    public static WorkoutSession.Log toLog(List<SessionExerciseDto> exercises) {
        return new WorkoutSession.Log(exercises.stream()
                .map((exercise) -> new WorkoutSession.Entry(exercise.exerciseId(), exercise.name(), note(exercise.note()),
                        // cardio has no sets
                        exercise.sets() == null || exercise.cardio() != null ? List.of()
                                : exercise.sets().stream()
                                        .map((set) -> new WorkoutSession.Set(set.reps(), set.weightKg(), set.rpe(), set.done(), set.restSeconds()))
                                        .toList(),
                        exercise.cardio() == null ? null
                                : new WorkoutSession.Cardio(Math.max(0, exercise.cardio().minutes()), exercise.cardio().done())))
                .toList());
    }

    /** Trimmed and cut to {@link #NOTE_LENGTH}, null when blank. */
    public static String note(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.strip();
        return trimmed.length() > NOTE_LENGTH ? trimmed.substring(0, NOTE_LENGTH) : trimmed;
    }
}

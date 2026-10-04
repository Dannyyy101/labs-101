package com.labs_101.backend.mapper;

import java.util.List;

import com.labs_101.backend.dtos.workout.session.SessionExerciseDto;
import com.labs_101.backend.dtos.workout.session.SessionSetDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.workout.WorkoutSession;

public class WorkoutSessionMapper {

    public static WorkoutSessionDto toDto(WorkoutSession session) {
        return new WorkoutSessionDto(
                session.getId(),
                session.getWorkout() != null ? session.getWorkout().getId() : null,
                session.getName(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getLog().exercises().stream()
                        .map((entry) -> new SessionExerciseDto(entry.exerciseId(), entry.name(),
                                entry.sets().stream()
                                        .map((set) -> new SessionSetDto(set.reps(), set.weightKg(), set.rpe(), set.done()))
                                        .toList()))
                        .toList());
    }

    public static WorkoutSession.Log toLog(List<SessionExerciseDto> exercises) {
        return new WorkoutSession.Log(exercises.stream()
                .map((exercise) -> new WorkoutSession.Entry(exercise.exerciseId(), exercise.name(),
                        exercise.sets() == null ? List.of()
                                : exercise.sets().stream()
                                        .map((set) -> new WorkoutSession.Set(set.reps(), set.weightKg(), set.rpe(), set.done()))
                                        .toList()))
                .toList());
    }
}

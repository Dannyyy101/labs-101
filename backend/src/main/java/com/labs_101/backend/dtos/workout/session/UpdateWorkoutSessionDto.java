package com.labs_101.backend.dtos.workout.session;

import java.util.List;

/** The current state of a running workout, it is saved while the user trains. */
public record UpdateWorkoutSessionDto(String name, List<SessionExerciseDto> exercises) {
}

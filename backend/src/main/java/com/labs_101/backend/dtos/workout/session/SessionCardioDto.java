package com.labs_101.backend.dtos.workout.session;

/** Cardio in a workout, only done cardio counts like only done sets. */
public record SessionCardioDto(int minutes, boolean done) {
}

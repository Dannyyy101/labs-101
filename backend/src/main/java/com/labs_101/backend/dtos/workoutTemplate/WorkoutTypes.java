package com.labs_101.backend.dtos.workoutTemplate;

public enum WorkoutTypes {
    STRENGTH_TRAINING("strength-training"),
    RUN_TRAINING("run-training")
    ;

    private final String text;

    /**
     * @param text
     */
    WorkoutTypes(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
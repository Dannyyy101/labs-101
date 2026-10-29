package com.labs_101.backend.dtos.overview;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/** @param activityType HKWorkoutActivityType, e.g. 37 for running */
public record OverviewWorkoutDto(
        UUID id,
        int activityType,
        Instant startDate,
        Instant endDate,
        double duration,
        @Nullable Double energy,
        @Nullable Double distance,
        @Nullable Double averageHeartRate) {
}

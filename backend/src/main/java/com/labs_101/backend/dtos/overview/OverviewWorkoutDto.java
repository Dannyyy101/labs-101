package com.labs_101.backend.dtos.overview;

import java.time.Instant;
import java.util.UUID;

/** @param activityType HKWorkoutActivityType, e.g. 37 for running */
public record OverviewWorkoutDto(
        UUID id,
        int activityType,
        Instant startDate,
        Instant endDate,
        double duration,
        Double energy,
        Double distance,
        Double averageHeartRate) {
}

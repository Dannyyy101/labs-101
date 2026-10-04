package com.labs_101.backend.dtos.run;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A running workout from Apple Health. Distances are in meters, times in
 * seconds and {@code duration} is the moving time (without pauses).
 *
 * @param zoneSeconds  time spent in each of the heart rate zones Z1–Z5
 * @param thumbnail    simplified track as [latitude, longitude], empty for indoor runs
 * @param bestEfforts  fastest time over the standard distances the run covered
 */
public record RunSummaryDto(
        UUID id,
        String name,
        Instant startDate,
        Instant endDate,
        String timeZone,
        boolean indoor,
        String sourceName,
        double distance,
        double duration,
        Double elevationGain,
        Double averageHeartRate,
        Double maxHeartRate,
        Double energy,
        Double cadence,
        double[] zoneSeconds,
        List<double[]> thumbnail,
        List<BestEffortDto> bestEfforts) {
}

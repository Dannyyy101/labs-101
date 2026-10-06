package com.labs_101.backend.dtos.overview;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the home page shows about one day, computed from the synced
 * Apple Health data. Times are in seconds, energy in kcal and distances in
 * meters.
 *
 * @param heartRate average heart rate of every 5 minutes of the day as [epoch millis, bpm]
 * @param history   the scores of the days up to and including {@code date}, oldest first
 */
public record HealthOverviewDto(
        LocalDate date,
        String timeZone,
        SleepDto sleep,
        RecoveryDto recovery,
        StrainDto strain,
        ActivityDto activity,
        List<double[]> heartRate,
        List<OverviewWorkoutDto> workouts,
        VitalsDto vitals,
        List<DayScoreDto> history) {
}

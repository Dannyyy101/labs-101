package com.labs_101.backend.dtos.overview;

/**
 * Cardiovascular load of the day.
 *
 * @param score        0–100, grows ever slower the higher the load
 * @param load         Banister TRIMP of the time above 30 % of the heart rate reserve
 * @param zoneSeconds  time in the zones Z1–Z5 (50, 60, 70, 80, 90 % of the maximum heart rate)
 * @param maxHeartRate the maximum heart rate the zones are based on
 */
public record StrainDto(int score, double load, double[] zoneSeconds, double maxHeartRate,
        Double averageHeartRate, Double peakHeartRate) {
}

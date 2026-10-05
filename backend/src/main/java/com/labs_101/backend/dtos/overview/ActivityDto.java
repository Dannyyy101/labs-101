package com.labs_101.backend.dtos.overview;

/**
 * Totals of the day, samples of several devices (iPhone and Watch) are not
 * counted twice.
 *
 * @param energyConsumed kcal eaten according to Apple Health, null if nothing was logged
 */
public record ActivityDto(
        double steps,
        double distance,
        double activeEnergy,
        double basalEnergy,
        double exerciseMinutes,
        int standHours,
        double flightsClimbed,
        double daylightMinutes,
        Double energyConsumed) {
}

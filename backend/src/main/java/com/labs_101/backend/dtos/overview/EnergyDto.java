package com.labs_101.backend.dtos.overview;

import java.time.LocalDate;

/**
 * kcal burned on a day, the food tracker adds them to the calorie goal.
 *
 * @param activeEnergy kcal burned by activity, without the basal energy
 */
public record EnergyDto(LocalDate date, double activeEnergy) {
}

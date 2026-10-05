package com.labs_101.backend.dtos.overview;

/**
 * Latest measurements up to the day.
 *
 * @param vo2Max           ml/(kg·min)
 * @param bodyMassChange   kg compared with 30 days before
 */
public record VitalsDto(Double vo2Max, Double bodyMass, Double bodyMassChange) {
}

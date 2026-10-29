package com.labs_101.backend.dtos.overview;

import org.jspecify.annotations.Nullable;

/**
 * Latest measurements up to the day.
 *
 * @param vo2Max           ml/(kg·min)
 * @param bodyMassChange   kg compared with 30 days before
 */
public record VitalsDto(@Nullable Double vo2Max, @Nullable Double bodyMass, @Nullable Double bodyMassChange) {
}

package com.labs_101.backend.dtos.health;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

/**
 * A quantity or category sample the app should write into HealthKit.
 * {@code unit} is a HealthKit unit string (e.g. "kg", "kcal", "count/min").
 */
public record CreateHealthWriteRequestDto(
        HealthSampleKind kind,
        String type,
        Instant startDate,
        Instant endDate,
        Double value,
        String unit,
        JsonNode metadata) {
}

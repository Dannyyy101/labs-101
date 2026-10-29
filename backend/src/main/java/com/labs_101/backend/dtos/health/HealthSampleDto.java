package com.labs_101.backend.dtos.health;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * One HealthKit sample, {@code uuid} is the UUID HealthKit assigned to it.
 * {@code value} and {@code unit} are set for quantity samples, category
 * samples only have a {@code value}. Everything else lives in {@code payload}.
 */
public record HealthSampleDto(
        UUID uuid,
        HealthSampleKind kind,
        String type,
        Instant startDate,
        Instant endDate,
        @Nullable Double value,
        @Nullable String unit,
        @Nullable String sourceName,
        @Nullable String sourceBundleId,
        @Nullable JsonNode payload) {
}

package com.labs_101.backend.dtos.health;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record HealthWriteRequestDto(
        Long id,
        HealthSampleKind kind,
        String type,
        Instant startDate,
        Instant endDate,
        @Nullable Double value,
        @Nullable String unit,
        @Nullable JsonNode metadata,
        HealthWriteRequestStatus status,
        @Nullable UUID sampleUuid,
        @Nullable String error,
        Instant createdAt) {
}

package com.labs_101.backend.dtos.health;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record HealthWriteRequestDto(
        Long id,
        HealthSampleKind kind,
        String type,
        Instant startDate,
        Instant endDate,
        Double value,
        String unit,
        JsonNode metadata,
        HealthWriteRequestStatus status,
        UUID sampleUuid,
        String error,
        Instant createdAt) {
}

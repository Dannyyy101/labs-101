package com.labs_101.backend.dtos.health;

import java.time.Instant;

public record HealthTypeSummaryDto(
        String type,
        HealthSampleKind kind,
        long count,
        Instant firstDate,
        Instant lastDate,
        Instant lastSyncedAt) {
}

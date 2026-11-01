package com.labs_101.backend.dtos.health;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record HealthTypeSummaryDto(
        String type,
        HealthSampleKind kind,
        long count,
        @Nullable Instant firstDate,
        @Nullable Instant lastDate,
        @Nullable Instant lastSyncedAt) {
}

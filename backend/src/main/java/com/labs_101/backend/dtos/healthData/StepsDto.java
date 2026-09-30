package com.labs_101.backend.dtos.healthData;

import java.time.Instant;

public record StepsDto(String uuid, String type, Integer value, Instant createdAt, Instant updatedAt)
        implements HealthDataItem {
}
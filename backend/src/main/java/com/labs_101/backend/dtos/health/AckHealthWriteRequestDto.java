package com.labs_101.backend.dtos.health;

import java.util.UUID;

/** Either the UUID HealthKit assigned to the written sample or why writing failed. */
public record AckHealthWriteRequestDto(UUID sampleUuid, String error) {
}

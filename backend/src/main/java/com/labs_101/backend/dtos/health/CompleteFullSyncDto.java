package com.labs_101.backend.dtos.health;

import java.util.UUID;

/**
 * Sent after all samples of {@code type} were uploaded with
 * {@code fullSyncId}. Samples of the type the run didn't upload were deleted
 * in HealthKit in the meantime and get removed.
 */
public record CompleteFullSyncDto(String type, UUID fullSyncId) {
}

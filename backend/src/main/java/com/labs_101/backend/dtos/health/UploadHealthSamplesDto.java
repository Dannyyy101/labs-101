package com.labs_101.backend.dtos.health;

import java.util.List;
import java.util.UUID;

/**
 * A batch of samples. {@code fullSyncId} is only set while the app syncs a
 * type from the beginning, see {@link CompleteFullSyncDto}.
 */
public record UploadHealthSamplesDto(UUID fullSyncId, List<HealthSampleDto> samples) {
}

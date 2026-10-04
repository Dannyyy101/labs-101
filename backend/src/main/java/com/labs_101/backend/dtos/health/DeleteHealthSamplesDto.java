package com.labs_101.backend.dtos.health;

import java.util.List;
import java.util.UUID;

public record DeleteHealthSamplesDto(List<UUID> uuids) {
}

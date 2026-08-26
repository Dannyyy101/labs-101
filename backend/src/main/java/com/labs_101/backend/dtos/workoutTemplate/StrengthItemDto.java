package com.labs_101.backend.dtos.workoutTemplate;

import java.util.List;

public record StrengthItemDto(String exerciseId, int order, String type, List<PlannedSetDto> sets) implements PlanItemDto {
}

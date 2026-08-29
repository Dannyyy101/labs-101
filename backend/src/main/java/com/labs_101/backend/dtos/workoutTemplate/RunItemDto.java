package com.labs_101.backend.dtos.workoutTemplate;

public record RunItemDto(String exerciseId, int order, String type) implements PlanItemDto {
}

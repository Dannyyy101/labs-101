package com.labs_101.backend.dtos.foodImage;

/** Photos uploaded today (UTC) and the daily limit, failed ones don't count. */
public record FoodImageUsageDto(long used, int limit) {
}

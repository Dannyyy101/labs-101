package com.labs_101.backend.dtos.overview;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

public record DayScoreDto(
        LocalDate date,
        @Nullable Integer recovery,
        @Nullable Integer strain,
        @Nullable Integer sleepScore,
        @Nullable Double sleep,
        @Nullable Double hrv,
        @Nullable Double restingHeartRate,
        double steps,
        double activeEnergy) {
}

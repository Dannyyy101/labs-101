package com.labs_101.backend.dtos.overview;

import java.time.LocalDate;

public record DayScoreDto(
        LocalDate date,
        Integer recovery,
        Integer strain,
        Integer sleepScore,
        Double sleep,
        Double hrv,
        Double restingHeartRate,
        double steps,
        double activeEnergy) {
}

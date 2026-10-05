package com.labs_101.backend.dtos.overview;

import java.time.Instant;
import java.util.List;

/**
 * The main sleep of the night before the day.
 *
 * @param asleep     time asleep of all sleeps ending on the day (naps included)
 * @param inBed      from falling asleep to waking up of the main sleep
 * @param need       how much sleep the score aims for
 * @param score      0–100 from duration, efficiency and deep + REM sleep
 */
public record SleepDto(
        Instant start,
        Instant end,
        double asleep,
        double inBed,
        double awake,
        double deep,
        double core,
        double rem,
        double need,
        int score,
        Double respiratoryRate,
        Double oxygenSaturation,
        List<SleepStageDto> stages) {
}

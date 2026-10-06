package com.labs_101.backend.dtos.overview;

import java.time.Instant;

/** @param stage AWAKE, CORE, DEEP, REM or ASLEEP (no stage recorded) */
public record SleepStageDto(String stage, Instant start, Instant end) {
}

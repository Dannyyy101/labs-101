package com.labs_101.backend.dtos.overview;

import org.jspecify.annotations.Nullable;

/**
 * How recovered the body is, the measurements of the night compared with the
 * averages of the 30 days before.
 *
 * @param score                     0–100, null until there is enough history
 * @param hrv                       heart rate variability (SDNN) during sleep in ms
 * @param restingHeartRate          in bpm
 * @param respiratoryRate           breaths per minute during sleep
 * @param oxygenSaturation          0–1 during sleep
 * @param wristTemperatureDeviation °C compared with the baseline
 */
public record RecoveryDto(
        @Nullable Integer score,
        @Nullable Double hrv,
        @Nullable Double hrvBaseline,
        @Nullable Double restingHeartRate,
        @Nullable Double restingHeartRateBaseline,
        @Nullable Double respiratoryRate,
        @Nullable Double respiratoryRateBaseline,
        @Nullable Double oxygenSaturation,
        @Nullable Double wristTemperatureDeviation) {
}

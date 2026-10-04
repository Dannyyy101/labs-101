package com.labs_101.backend.dtos.run;

/**
 * Point of the track, resampled to a fixed distance.
 *
 * @param distance  meters since the start
 * @param time      moving time since the start in seconds
 * @param pace      smoothed pace in seconds per kilometer
 * @param heartRate beats per minute, null without heart rate samples
 */
public record RunPointDto(
        double distance,
        double time,
        double latitude,
        double longitude,
        Double elevation,
        Double pace,
        Double heartRate) {
}

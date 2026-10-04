package com.labs_101.backend.dtos.run;

/**
 * One kilometer of a run, the last one may be shorter.
 *
 * @param distance length of the split in meters
 * @param time     moving time of the split in seconds
 */
public record RunSplitDto(int index, double distance, double time, Double heartRate) {
}

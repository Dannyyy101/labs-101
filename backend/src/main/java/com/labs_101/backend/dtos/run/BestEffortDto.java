package com.labs_101.backend.dtos.run;

/** Fastest {@code distance} meters within a run, {@code time} in seconds. */
public record BestEffortDto(String name, double distance, double time) {
}

package com.labs_101.backend.dtos.profile;

import java.time.LocalDate;

/**
 * Progress towards one achievement, texts and icons are up to the client.
 * For {@code SUB_25} the progress is the best 5 km time in seconds and lower
 * is better, for all others higher.
 *
 * @param progress   null while there is nothing to compare yet (e.g. no 5 km run)
 * @param achievedAt local day the target was first reached, null while it is open
 */
public record AchievementDto(String id, Double progress, double target, LocalDate achievedAt) {
}

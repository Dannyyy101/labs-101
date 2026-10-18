package com.labs_101.backend.dtos.friend;

import java.time.Instant;

/**
 * The workout a friend is doing right now.
 *
 * @param name    of the template it was started from, null for a free workout
 * @param cheered the signed in user already cheered this workout on, it can only be done once
 */
public record TrainingDto(Long sessionId, String name, Instant startedAt, boolean cheered) {
}

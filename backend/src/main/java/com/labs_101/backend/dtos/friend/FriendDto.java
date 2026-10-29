package com.labs_101.backend.dtos.friend;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * @param since    the request was accepted
 * @param training null while the friend isn't training
 */
public record FriendDto(String id, @Nullable String name, @Nullable String image, Instant since, @Nullable TrainingDto training) {
}

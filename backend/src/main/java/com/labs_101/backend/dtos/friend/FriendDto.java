package com.labs_101.backend.dtos.friend;

import java.time.Instant;

/**
 * @param since    the request was accepted
 * @param training null while the friend isn't training
 */
public record FriendDto(String id, String name, String image, Instant since, TrainingDto training) {
}

package com.labs_101.backend.dtos.friend;

import java.time.Instant;

import com.labs_101.backend.dtos.calendar.CalendarUserDto;

/** An open request, {@code user} is the other one: who sent it (incoming) or who it was sent to (outgoing). */
public record FriendRequestDto(Long id, CalendarUserDto user, Instant createdAt) {
}

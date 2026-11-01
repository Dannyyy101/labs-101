package com.labs_101.backend.dtos.calendar;

import org.jspecify.annotations.Nullable;

/** What the calendar shows of a person: name and profile picture. */
public record CalendarUserDto(String id, @Nullable String name, @Nullable String image) {
}

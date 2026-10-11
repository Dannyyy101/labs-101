package com.labs_101.backend.dtos.calendar;

/** {@code name} is optional, the feed's own name is taken without it. */
public record CreateExternalCalendarDto(String name, String url) {
}

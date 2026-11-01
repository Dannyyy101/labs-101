package com.labs_101.backend.dtos.calendar;

import org.jspecify.annotations.Nullable;

/** {@code name} is optional, the feed's own name is taken without it. */
public record CreateExternalCalendarDto(@Nullable String name, String url) {
}

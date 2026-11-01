package com.labs_101.backend.dtos.calendar;

import org.jspecify.annotations.Nullable;

/**
 * {@code error} says why the events couldn't be read the last time, null when they could. {@code accountId} and
 * {@code appleId} are null for a shared calendar.
 */
public record ExternalCalendarDto(Long id, String name, String url, @Nullable String error, @Nullable Long accountId, @Nullable String appleId) {
}

package com.labs_101.backend.dtos.calendar;

/**
 * {@code error} says why the events couldn't be read the last time, null when they could. {@code accountId} and
 * {@code appleId} are null for a shared calendar.
 */
public record ExternalCalendarDto(Long id, String name, String url, String error, Long accountId, String appleId) {
}

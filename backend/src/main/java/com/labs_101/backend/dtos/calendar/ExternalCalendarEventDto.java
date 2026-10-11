package com.labs_101.backend.dtos.calendar;

import java.time.Instant;

/**
 * One occurrence of an event of an external calendar. An all-day event starts at midnight of its first day and
 * ends at midnight after its last day, in german time.
 */
public record ExternalCalendarEventDto(Long calendarId, String calendarName, String uid, String title,
        Instant startDate, Instant endDate, boolean allDay) {
}

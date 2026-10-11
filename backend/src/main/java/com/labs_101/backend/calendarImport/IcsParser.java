package com.labs_101.backend.calendarImport;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.io.TimezoneInfo;
import biweekly.property.DateStart;
import biweekly.util.DateTimeComponents;
import biweekly.util.ICalDate;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;

/**
 * Reads the events of an iCalendar feed. Repeating events are expanded with their exceptions (EXDATE) and
 * changed occurrences (RECURRENCE-ID), the way Apple Calendar exports them.
 */
public class IcsParser {

    /** Floating times and all-day events are in german time, the users are in germany. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    // a daily event in a window of several years, more than any calendar view shows
    private static final int MAX_OCCURRENCES = 1000;

    public record ImportedEvent(String uid, String title, Instant startDate, Instant endDate, boolean allDay) {
    }

    /** A parsed feed, {@link #name} is its X-WR-CALNAME. */
    public record Feed(String name, List<ICalendar> calendars) {
    }

    /** Throws when the text is no iCalendar. */
    public static Feed parse(String text) {
        List<ICalendar> calendars = Biweekly.parse(text).all();
        if (calendars.isEmpty()) {
            throw new IllegalArgumentException("No iCalendar data");
        }
        ICalendar first = calendars.getFirst();
        String name = first.getExperimentalProperty("X-WR-CALNAME") == null ? null
                : first.getExperimentalProperty("X-WR-CALNAME").getValue();
        return new Feed(name, calendars);
    }

    /** The occurrences that overlap the window, ordered by their start. */
    public static List<ImportedEvent> events(Feed feed, Instant windowStart, Instant windowEnd) {
        List<ImportedEvent> events = new ArrayList<>();
        for (ICalendar calendar : feed.calendars()) {
            TimezoneInfo timezones = calendar.getTimezoneInfo();
            // occurrences of repeating events that were changed, they are events of their own
            Set<String> changed = new HashSet<>();
            for (VEvent event : calendar.getEvents()) {
                if (event.getRecurrenceId() != null && event.getRecurrenceId().getValue() != null) {
                    ICalDate recurrenceId = event.getRecurrenceId().getValue();
                    changed.add(recurrenceId.hasTime() ? key(uid(event), recurrenceId.getTime())
                            : key(uid(event), date(recurrenceId)));
                }
            }
            for (VEvent event : calendar.getEvents()) {
                if (event.getDateStart() == null || event.getDateStart().getValue() == null
                        || (event.getStatus() != null && event.getStatus().isCancelled())) {
                    continue;
                }
                occurrences(event, timezones, changed, windowStart, windowEnd, events);
            }
        }
        events.sort((a, b) -> a.startDate().compareTo(b.startDate()));
        return events;
    }

    private static void occurrences(VEvent event, TimezoneInfo timezones, Set<String> changed, Instant windowStart,
            Instant windowEnd, List<ImportedEvent> events) {
        DateStart dateStart = event.getDateStart();
        ICalDate start = dateStart.getValue();
        boolean allDay = !start.hasTime();
        String uid = uid(event);
        String title = event.getSummary() == null ? null : event.getSummary().getValue();
        TimeZone timezone = allDay ? TimeZone.getTimeZone(ZONE) : timezone(timezones, dateStart);

        boolean repeats = event.getRecurrenceId() == null
                && (event.getRecurrenceRule() != null || !event.getRecurrenceDates().isEmpty());
        if (!repeats) {
            add(event, uid, title, start, allDay, windowStart, windowEnd, events);
            return;
        }

        Duration duration = duration(event, allDay);
        DateIterator iterator = event.getDateIterator(timezone);
        // skip the occurrences that end before the window, an all-day event a day more as it is moved to german time
        iterator.advanceTo(Date.from(windowStart.minus(duration).minus(Duration.ofDays(1))));
        for (int count = 0; iterator.hasNext() && count < MAX_OCCURRENCES;) {
            Date occurrence = iterator.next();
            Instant occurrenceStart = allDay ? startOfDay(occurrence, timezone) : occurrence.toInstant();
            if (changed.contains(allDay ? key(uid, occurrenceStart.atZone(ZONE).toLocalDate())
                    : key(uid, occurrence.getTime()))) {
                continue;
            }
            if (occurrenceStart.isAfter(windowEnd)) {
                break;
            }
            Instant occurrenceEnd = allDay
                    ? occurrenceStart.atZone(ZONE).plusDays(Math.max(1, duration.toDays())).toInstant()
                    : occurrenceStart.plus(duration);
            if (!occurrenceEnd.isBefore(windowStart)) {
                events.add(new ImportedEvent(uid, title, occurrenceStart, occurrenceEnd, allDay));
                count++;
            }
        }
    }

    private static void add(VEvent event, String uid, String title, ICalDate start, boolean allDay,
            Instant windowStart, Instant windowEnd, List<ImportedEvent> events) {
        Instant startDate;
        Instant endDate;
        if (allDay) {
            LocalDate day = date(start);
            startDate = day.atStartOfDay(ZONE).toInstant();
            endDate = day.plusDays(Math.max(1, duration(event, true).toDays())).atStartOfDay(ZONE).toInstant();
        } else {
            startDate = start.toInstant();
            endDate = startDate.plus(duration(event, false));
        }
        if (!startDate.isAfter(windowEnd) && !endDate.isBefore(windowStart)) {
            events.add(new ImportedEvent(uid, title, startDate, endDate, allDay));
        }
    }

    /** From DTEND or DURATION, an event without them takes a day when it's an all-day event, otherwise no time. */
    private static Duration duration(VEvent event, boolean allDay) {
        ICalDate start = event.getDateStart().getValue();
        if (event.getDateEnd() != null && event.getDateEnd().getValue() != null) {
            ICalDate end = event.getDateEnd().getValue();
            if (allDay) {
                return Duration.ofDays(Math.max(1, date(end).toEpochDay() - date(start).toEpochDay()));
            }
            return Duration.ofMillis(Math.max(0, end.getTime() - start.getTime()));
        }
        if (event.getDuration() != null && event.getDuration().getValue() != null) {
            return Duration.ofMillis(event.getDuration().getValue().toMillis());
        }
        return allDay ? Duration.ofDays(1) : Duration.ZERO;
    }

    /** The time zone of the TZID, UTC for times ending with Z, german time for floating times. */
    private static TimeZone timezone(TimezoneInfo timezones, DateStart dateStart) {
        TimezoneAssignment assignment = timezones.getTimezone(dateStart);
        if (assignment != null) {
            return assignment.getTimeZone();
        }
        DateTimeComponents raw = dateStart.getValue().getRawComponents();
        boolean utc = raw != null && raw.isUtc();
        return TimeZone.getTimeZone(utc ? ZoneOffset.UTC : ZONE);
    }

    /** The day as written in the feed, a date without time has no time zone. */
    private static LocalDate date(ICalDate date) {
        DateTimeComponents raw = date.getRawComponents();
        if (raw != null) {
            return LocalDate.of(raw.getYear(), raw.getMonth(), raw.getDate());
        }
        return date.toInstant().atZone(ZONE).toLocalDate();
    }

    private static Instant startOfDay(Date date, TimeZone timezone) {
        return date.toInstant().atZone(timezone.toZoneId()).toLocalDate().atStartOfDay(ZONE).toInstant();
    }

    private static String uid(VEvent event) {
        return event.getUid() == null ? null : event.getUid().getValue();
    }

    private static String key(String uid, long time) {
        return uid + "@" + time;
    }

    private static String key(String uid, LocalDate date) {
        return uid + "@" + date;
    }
}

package com.labs_101.backend.calendarImport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.labs_101.backend.calendarImport.IcsParser.ImportedEvent;

public class IcsParserTest {

    // like Apple exports a shared calendar: a weekly event at 18:00 german time, one week left out, one moved
    private static final String ICS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//iCloud//EN
            X-WR-CALNAME:Privat
            BEGIN:VTIMEZONE
            TZID:Europe/Berlin
            BEGIN:DAYLIGHT
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            DTSTART:19810329T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
            TZNAME:MESZ
            END:DAYLIGHT
            BEGIN:STANDARD
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            DTSTART:19961027T030000
            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU
            TZNAME:MEZ
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VEVENT
            UID:choir
            SUMMARY:Chor
            DTSTART;TZID=Europe/Berlin:20261007T180000
            DTEND;TZID=Europe/Berlin:20261007T193000
            RRULE:FREQ=WEEKLY
            EXDATE;TZID=Europe/Berlin:20261014T180000
            END:VEVENT
            BEGIN:VEVENT
            UID:choir
            RECURRENCE-ID;TZID=Europe/Berlin:20261028T180000
            SUMMARY:Chor (Generalprobe)
            DTSTART;TZID=Europe/Berlin:20261028T170000
            DTEND;TZID=Europe/Berlin:20261028T200000
            END:VEVENT
            BEGIN:VEVENT
            UID:trip
            SUMMARY:Urlaub
            DTSTART;VALUE=DATE:20261030
            DTEND;VALUE=DATE:20261102
            END:VEVENT
            BEGIN:VEVENT
            UID:dentist
            SUMMARY:Zahnarzt
            DTSTART:20261020T080000Z
            DTEND:20261020T090000Z
            STATUS:CANCELLED
            END:VEVENT
            END:VCALENDAR
            """.replace("\n", "\r\n");

    @Test
    void testRepeatingEventWithExceptions() {
        List<ImportedEvent> events = events("2026-10-01T00:00:00Z", "2026-11-06T00:00:00Z").stream()
                .filter((e) -> e.uid().equals("choir")).toList();

        assertEquals(List.of(
                // the 14th is left out, after october 25th it's winter time
                new ImportedEvent("choir", "Chor", Instant.parse("2026-10-07T16:00:00Z"), Instant.parse("2026-10-07T17:30:00Z"), false),
                new ImportedEvent("choir", "Chor", Instant.parse("2026-10-21T16:00:00Z"), Instant.parse("2026-10-21T17:30:00Z"), false),
                new ImportedEvent("choir", "Chor (Generalprobe)", Instant.parse("2026-10-28T16:00:00Z"), Instant.parse("2026-10-28T19:00:00Z"), false),
                new ImportedEvent("choir", "Chor", Instant.parse("2026-11-04T17:00:00Z"), Instant.parse("2026-11-04T18:30:00Z"), false)),
                events);
    }

    @Test
    void testOnlyTheWindow() {
        List<ImportedEvent> events = events("2027-03-01T00:00:00Z", "2027-03-08T00:00:00Z");

        assertEquals(List.of(Instant.parse("2027-03-03T17:00:00Z")), events.stream().map(ImportedEvent::startDate).toList());
    }

    @Test
    void testAllDayEventInGermanTime() {
        ImportedEvent trip = events("2026-10-31T00:00:00Z", "2026-10-31T12:00:00Z").stream()
                .filter((e) -> e.uid().equals("trip")).findFirst().orElseThrow();

        assertTrue(trip.allDay());
        // german midnight, not utc, of the 30th until the end of november 1st
        assertEquals(Instant.parse("2026-10-29T23:00:00Z"), trip.startDate());
        assertEquals(Instant.parse("2026-11-01T23:00:00Z"), trip.endDate());
    }

    @Test
    void testCancelledEventsAreLeftOut() {
        assertFalse(events("2026-10-20T00:00:00Z", "2026-10-21T00:00:00Z").stream()
                .anyMatch((e) -> e.uid().equals("dentist")));
    }

    @Test
    void testName() {
        assertEquals("Privat", IcsParser.parse(ICS).name());
    }

    @Test
    void testNoCalendar() {
        assertThrows(RuntimeException.class, () -> IcsParser.parse("<html>Not found</html>"));
    }

    @Test
    void testOnlyHttpsLinksOfAllowedHosts() {
        IcsClient client = new IcsClient("icloud.com");

        assertEquals(URI.create("https://p42-caldav.icloud.com/published/2/abc"),
                client.normalize("webcal://p42-caldav.icloud.com/published/2/abc"));
        assertEquals(URI.create("https://p42-caldav.icloud.com/published/2/abc"),
                client.normalize(" https://p42-caldav.icloud.com/published/2/abc "));
        assertNull(client.normalize("http://p42-caldav.icloud.com/published/2/abc"));
        assertNull(client.normalize("https://icloud.com.example.org/calendar"));
        assertNull(client.normalize("https://evilicloud.com/calendar"));
        assertNull(client.normalize("https://user@p42-caldav.icloud.com/calendar"));
        assertNull(client.normalize("https://localhost:8080/api"));
        assertNull(client.normalize("kein link"));
    }

    private static List<ImportedEvent> events(String start, String end) {
        return IcsParser.events(IcsParser.parse(ICS), Instant.parse(start), Instant.parse(end));
    }
}

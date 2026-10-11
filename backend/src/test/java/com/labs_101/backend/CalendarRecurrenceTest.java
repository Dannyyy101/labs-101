package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;
import com.labs_101.backend.entities.Recurrence;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.ZitadelClient;
import com.labs_101.backend.services.CalendarService;
import com.labs_101.backend.services.NotificationService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ CalendarService.class, NotificationService.class, ZitadelClient.class })
@TestPropertySource(properties = { "auth.issuer=https://auth.example.com", "auth.organization-id=org-1" })
public class CalendarRecurrenceTest {

    // monday 19:00 in germany
    private static final Instant START = Instant.parse("2026-10-12T17:00:00Z");
    private static final Instant END = Instant.parse("2026-10-12T18:00:00Z");

    @Autowired
    private CalendarService calendarService;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID().toString());
        user.setName("Daniel");
        user = userRepository.saveAndFlush(user);
    }

    @Test
    void testWeeklyEventOccursEveryWeekAtTheSameLocalTime() {
        create(Recurrence.WEEKLY, null);

        // the clocks go back on october 25th, 19:00 is 18:00 utc then
        assertEquals(List.of(START, Instant.parse("2026-10-19T17:00:00Z"), Instant.parse("2026-10-26T18:00:00Z"),
                Instant.parse("2026-11-02T18:00:00Z")),
                starts(Instant.parse("2026-10-12T00:00:00Z"), Instant.parse("2026-11-08T00:00:00Z")));
    }

    @Test
    void testOccurrencesLongAfterTheFirst() {
        create(Recurrence.DAILY, null);

        List<CalendarEventDto> events = calendarService.getAllCalendarEvents(user.getId(),
                Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-03T00:00:00Z"));

        // still 19:00, in winter that's 18:00 utc
        assertEquals(List.of(Instant.parse("2030-01-01T18:00:00Z"), Instant.parse("2030-01-02T18:00:00Z")),
                events.stream().map(CalendarEventDto::getStartDate).toList());
        assertEquals(Instant.parse("2030-01-01T19:00:00Z"), events.getFirst().getEndDate());
    }

    @Test
    void testNoOccurrenceAfterTheEnd() {
        create(Recurrence.DAILY, Instant.parse("2026-10-14T21:59:59Z"));

        assertEquals(3, starts(START, Instant.parse("2026-10-31T00:00:00Z")).size());
    }

    @Test
    void testMonthlyKeepsTheDay() {
        calendarService.createCalendarEvent(new CreateCalendarEventDto("Messen", Instant.parse("2027-01-31T08:00:00Z"),
                Instant.parse("2027-01-31T09:00:00Z"), user.getId(), null, List.of(), Recurrence.MONTHLY, null));

        assertEquals(List.of(Instant.parse("2027-01-31T08:00:00Z"), Instant.parse("2027-02-28T08:00:00Z"),
                Instant.parse("2027-03-31T07:00:00Z")),
                starts(Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-04-01T00:00:00Z")));
    }

    @Test
    void testEditingAnOccurrenceMovesTheSeries() {
        CalendarEventDto event = create(Recurrence.WEEKLY, null);
        Instant occurrence = Instant.parse("2026-10-19T17:00:00Z");

        // the second week an hour later
        calendarService.updateCalendarEvent(new UpdateCalendarEventDto(event.getId(), "Push",
                occurrence.plusSeconds(3600), occurrence.plusSeconds(7200), null, null, Recurrence.WEEKLY, null,
                occurrence));

        assertEquals(List.of(START.plusSeconds(3600), occurrence.plusSeconds(3600)),
                starts(Instant.parse("2026-10-12T00:00:00Z"), Instant.parse("2026-10-25T00:00:00Z")));
    }

    @Test
    void testRemovingTheRecurrence() {
        CalendarEventDto event = create(Recurrence.DAILY, null);

        calendarService.updateCalendarEvent(
                new UpdateCalendarEventDto(event.getId(), "Push", null, null, null, null, null, null, null));

        assertEquals(List.of(START), starts(START, Instant.parse("2026-10-31T00:00:00Z")));
    }

    private CalendarEventDto create(Recurrence recurrence, Instant recurrenceEnd) {
        return calendarService.createCalendarEvent(
                new CreateCalendarEventDto("Push", START, END, user.getId(), null, List.of(), recurrence, recurrenceEnd));
    }

    private List<Instant> starts(Instant windowStart, Instant windowEnd) {
        return calendarService.getAllCalendarEvents(user.getId(), windowStart, windowEnd).stream()
                .map(CalendarEventDto::getStartDate).toList();
    }
}

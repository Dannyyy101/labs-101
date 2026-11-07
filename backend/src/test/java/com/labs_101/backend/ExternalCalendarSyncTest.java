package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.labs_101.backend.calendarImport.CalDavClient;
import com.labs_101.backend.calendarImport.CredentialCipher;
import com.labs_101.backend.calendarImport.IcsClient;
import com.labs_101.backend.dtos.calendar.ExternalCalendarEventDto;
import com.labs_101.backend.entities.ExternalCalendar;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.ExternalCalendarRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.ExternalCalendarService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ ExternalCalendarService.class })
public class ExternalCalendarSyncTest {

    private static final Instant START = Instant.parse("2026-11-02T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-09T00:00:00Z");

    @Autowired
    private ExternalCalendarService externalCalendarService;
    @Autowired
    private ExternalCalendarRepository externalCalendarRepository;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private IcsClient icsClient;
    @MockitoBean
    private CalDavClient calDavClient;
    @MockitoBean
    private CredentialCipher credentialCipher;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID().toString());
        user.setName("Daniel");
        user = userRepository.saveAndFlush(user);
        URI uri = URI.create("https://p01-caldav.icloud.com/shared.ics");
        when(icsClient.normalize(any())).thenReturn(uri);
        when(icsClient.isAllowed(any())).thenReturn(true);
        externalCalendarRepository.saveAndFlush(
                new ExternalCalendar(null, user, "Privat", uri.toString(), Instant.now(), null));
    }

    @Test
    void testEventsAreStoredAndShownWithoutApple() {
        when(icsClient.download(any())).thenReturn(ics("Laufen"));

        assertEquals(List.of(), titles(externalCalendarService.getEvents(user.getId(), START, END)));
        assertEquals(List.of("Laufen"), titles(externalCalendarService.syncEvents(user.getId(), START, END)));

        when(icsClient.download(any())).thenThrow(new IllegalStateException("Apple is down"));
        assertEquals(List.of("Laufen"), titles(externalCalendarService.getEvents(user.getId(), START, END)));
    }

    @Test
    void testSyncReplacesTheEventsOfTheWindow() {
        when(icsClient.download(any())).thenReturn(ics("Laufen"));
        externalCalendarService.syncEvents(user.getId(), START, END);

        // in memory for a few minutes, a new sync would get the same feed
        ExternalCalendar calendar = externalCalendarRepository.findByUser_IdOrderByCreatedAt(user.getId()).getFirst();
        externalCalendarService.deleteCalendar(user.getId(), calendar.getId());
        URI uri = URI.create("https://p01-caldav.icloud.com/other.ics");
        when(icsClient.normalize(any())).thenReturn(uri);
        when(icsClient.download(any())).thenReturn(ics("Schwimmen"));
        externalCalendarRepository.saveAndFlush(
                new ExternalCalendar(null, user, "Privat", uri.toString(), Instant.now(), null));

        assertEquals(List.of("Schwimmen"), titles(externalCalendarService.syncEvents(user.getId(), START, END)));
        assertEquals(List.of("Schwimmen"), titles(externalCalendarService.syncEvents(user.getId(), START, END)));
    }

    private static List<String> titles(List<ExternalCalendarEventDto> events) {
        return events.stream().map(ExternalCalendarEventDto::title).toList();
    }

    private static String ics(String title) {
        return """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Apple Inc.//iCloud//EN
                BEGIN:VEVENT
                UID:event-1
                DTSTART:20261104T170000Z
                DTEND:20261104T180000Z
                SUMMARY:%s
                END:VEVENT
                END:VCALENDAR
                """.formatted(title).replace("\n", "\r\n");
    }
}

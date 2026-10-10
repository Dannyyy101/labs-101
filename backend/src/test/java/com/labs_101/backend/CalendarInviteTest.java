package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.ZitadelClient;
import com.labs_101.backend.services.CalendarService;
import com.labs_101.backend.services.NotificationService;
import com.labs_101.backend.services.UserService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ CalendarService.class, NotificationService.class, UserService.class, ZitadelClient.class })
@TestPropertySource(properties = { "auth.issuer=https://auth.example.com", "auth.organization-id=org-1" })
public class CalendarInviteTest {

    private static final Instant START = Instant.parse("2026-10-12T17:00:00Z");
    private static final Instant END = Instant.parse("2026-10-12T18:00:00Z");

    @Autowired
    private CalendarService calendarService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    private User creator;
    private User anna;
    private User ben;

    @BeforeEach
    void setUp() {
        creator = user("Daniel", "https://example.com/daniel.png");
        anna = user("Anna Schmidt", "https://example.com/anna.png");
        ben = user("Ben", null);
    }

    @Test
    void testInviteesSeeTheEventAndGetNotified() {
        CalendarEventDto event = create(List.of(anna.getId()));

        // the pictures come from zitadel, not from the stored image
        assertEquals(List.of(new CalendarUserDto(anna.getId(), "Anna Schmidt", avatar(anna))),
                event.getInvitees());
        assertEquals(avatar(creator), event.getCreator().image());
        assertEquals(List.of(event.getId()), eventIds(anna));
        assertEquals(List.of(event.getId()), eventIds(creator));
        assertEquals(List.of(), eventIds(ben));
        assertEquals(1, notificationService.countUnread(anna.getId()));
        assertEquals(0, notificationService.countUnread(creator.getId()));
    }

    @Test
    void testCreatorAndDuplicatesAreNotInvited() {
        CalendarEventDto event = create(List.of(creator.getId(), anna.getId(), anna.getId()));

        assertEquals(List.of(anna.getId()), event.getInvitees().stream().map(CalendarUserDto::id).toList());
        // listed once although the creator is invited too
        assertEquals(1, eventIds(creator).size());
    }

    @Test
    void testUpdateNotifiesOnlyNewInvitees() {
        CalendarEventDto event = create(List.of(anna.getId()));

        CalendarEventDto updated = calendarService.updateCalendarEvent(
                new UpdateCalendarEventDto(event.getId(), null, null, null, null, List.of(anna.getId(), ben.getId())));

        assertEquals(List.of(anna.getId(), ben.getId()), updated.getInvitees().stream().map(CalendarUserDto::id).toList());
        assertEquals(1, notificationService.countUnread(anna.getId()));
        assertEquals(1, notificationService.countUnread(ben.getId()));
    }

    @Test
    void testUpdateWithoutInviteesKeepsThem() {
        CalendarEventDto event = create(List.of(anna.getId()));

        CalendarEventDto updated = calendarService.updateCalendarEvent(
                new UpdateCalendarEventDto(event.getId(), "Beine", null, null, null, null));

        assertEquals(1, updated.getInvitees().size());
    }

    @Test
    void testRemovedInviteeNoLongerSeesTheEvent() {
        CalendarEventDto event = create(List.of(anna.getId()));

        calendarService.updateCalendarEvent(new UpdateCalendarEventDto(event.getId(), null, null, null, null, List.of()));

        assertEquals(List.of(), eventIds(anna));
    }

    @Test
    void testUnknownInvitee() {
        assertThrows(NotFoundException.class, () -> create(List.of("missing")));
    }

    @Test
    void testSearchUsers() {
        assertTrue(userService.search("schmidt").stream().anyMatch((u) -> u.id().equals(anna.getId())));
        assertTrue(userService.search(anna.getEmail().toUpperCase()).stream().anyMatch((u) -> u.id().equals(anna.getId())));
        assertEquals(List.of(), userService.search(" "));
    }

    private CalendarEventDto create(List<String> inviteeIds) {
        return calendarService.createCalendarEvent(
                new CreateCalendarEventDto("Push", START, END, creator.getId(), null, inviteeIds));
    }

    private List<Long> eventIds(User user) {
        return calendarService.getAllCalendarEvents(user.getId(), START.minusSeconds(3600), END.plusSeconds(3600))
                .stream().map(CalendarEventDto::getId).toList();
    }

    private static String avatar(User user) {
        return "https://auth.example.com/assets/v1/org-1/users/" + user.getId() + "/avatar";
    }

    private User user(String name, String image) {
        User user = new User(UUID.randomUUID().toString());
        user.setName(name);
        user.setEmail(user.getId() + "@example.com");
        user.setImage(image);
        return userRepository.saveAndFlush(user);
    }
}

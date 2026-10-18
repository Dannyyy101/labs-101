package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.labs_101.backend.dtos.friend.FriendDto;
import com.labs_101.backend.dtos.friend.FriendProfileDto;
import com.labs_101.backend.dtos.friend.FriendsDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.repositories.ProfileRepository;
import com.labs_101.backend.repositories.RunRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.ZitadelClient;
import com.labs_101.backend.services.FriendService;
import com.labs_101.backend.services.NotificationService;
import com.labs_101.backend.services.ProfileService;
import com.labs_101.backend.services.RunService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ FriendService.class, NotificationService.class, ProfileService.class, ProfileRepository.class,
        LoginDayRepository.class, RunService.class, RunRepository.class, ZitadelClient.class })
@TestPropertySource(properties = { "auth.issuer=https://auth.example.com", "auth.organization-id=org-1" })
public class FriendTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    @Autowired
    private FriendService friendService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private User anna;
    private User ben;
    private User carla;

    @BeforeEach
    void setUp() {
        anna = user("Anna");
        ben = user("Ben");
        carla = user("Carla");
    }

    @Test
    void testRequestAndAccept() {
        friendService.sendRequest(anna.getId(), ben.getId());

        FriendsDto annas = friendService.getFriends(anna.getId());
        assertEquals(List.of(), annas.friends());
        assertEquals(ben.getId(), annas.outgoing().getFirst().user().id());
        FriendsDto bens = friendService.getFriends(ben.getId());
        assertEquals(anna.getId(), bens.incoming().getFirst().user().id());
        assertEquals("Anna möchte mit dir befreundet sein", lastNotification(ben).getTitle());

        // only the one it was sent to can accept it
        Long requestId = bens.incoming().getFirst().id();
        assertThrows(NotFoundException.class, () -> friendService.acceptRequest(anna.getId(), requestId));
        friendService.acceptRequest(ben.getId(), requestId);

        assertEquals(List.of(ben.getId()), friendIds(anna));
        assertEquals(List.of(anna.getId()), friendIds(ben));
        assertEquals(List.of(), friendService.getFriends(ben.getId()).incoming());
        assertEquals("/friends/" + ben.getId(), lastNotification(anna).getLink());
    }

    @Test
    void testRequestBackAccepts() {
        friendService.sendRequest(anna.getId(), ben.getId());
        friendService.sendRequest(ben.getId(), anna.getId());

        assertEquals(List.of(ben.getId()), friendIds(anna));
        assertThrows(BadRequestException.class, () -> friendService.sendRequest(anna.getId(), ben.getId()));
        assertThrows(BadRequestException.class, () -> friendService.sendRequest(anna.getId(), anna.getId()));
    }

    @Test
    void testDeclineAndRemove() {
        friendService.sendRequest(anna.getId(), ben.getId());
        Long requestId = friendService.getFriends(ben.getId()).incoming().getFirst().id();
        // someone else can't touch it
        assertThrows(NotFoundException.class, () -> friendService.deleteRequest(carla.getId(), requestId));
        friendService.deleteRequest(ben.getId(), requestId);
        assertEquals(List.of(), friendService.getFriends(anna.getId()).outgoing());

        befriend(anna, ben);
        friendService.removeFriend(ben.getId(), anna.getId());
        assertEquals(List.of(), friendIds(anna));
    }

    @Test
    void testOnlyFriendsSeeTheProfile() {
        befriend(anna, ben);
        LocalDate today = LocalDate.now(ZONE);

        FriendProfileDto profile = friendService.getProfile(anna.getId(), ben.getId(), today, ZONE);
        assertEquals("Ben", profile.friend().name());
        assertEquals("https://auth.example.com/assets/v1/org-1/users/" + ben.getId() + "/avatar",
                profile.friend().image());
        assertNotNull(profile.profile().achievements());

        assertThrows(NotFoundException.class, () -> friendService.getProfile(carla.getId(), ben.getId(), today, ZONE));
        // an open request isn't enough
        friendService.sendRequest(carla.getId(), ben.getId());
        assertThrows(NotFoundException.class, () -> friendService.getProfile(carla.getId(), ben.getId(), today, ZONE));
    }

    @Test
    void testCheerOncePerWorkout() {
        befriend(anna, ben);
        assertNull(friend(anna, ben).training());
        assertThrows(BadRequestException.class, () -> friendService.cheer(anna.getId(), ben.getId()));

        long sessionId = session(ben, "Push", Instant.now().minus(Duration.ofMinutes(20)));
        FriendDto training = friend(anna, ben);
        assertEquals(sessionId, training.training().sessionId());
        assertEquals("Push", training.training().name());
        assertFalse(training.training().cheered());

        friendService.cheer(anna.getId(), ben.getId());
        assertTrue(friend(anna, ben).training().cheered());
        NotificationDto notification = lastNotification(ben);
        assertEquals("🔥 Anna feuert dich an!", notification.getTitle());
        assertEquals("Gib alles bei Push!", notification.getMessage());
        assertThrows(BadRequestException.class, () -> friendService.cheer(anna.getId(), ben.getId()));

        // not friends
        assertThrows(NotFoundException.class, () -> friendService.cheer(carla.getId(), ben.getId()));

        // the next workout can be cheered on again
        jdbc.update("UPDATE workout_session SET ended_at = now() WHERE id = ?", sessionId);
        assertNull(friend(anna, ben).training());
        session(ben, null, Instant.now());
        assertFalse(friend(anna, ben).training().cheered());
        friendService.cheer(anna.getId(), ben.getId());
    }

    @Test
    void testForgottenSessionIsNoTraining() {
        befriend(anna, ben);
        session(ben, "Pull", Instant.now().minus(Duration.ofHours(7)));
        assertNull(friend(anna, ben).training());
    }

    @Test
    void testTrainingFriendsFirst() {
        befriend(anna, ben);
        befriend(anna, carla);
        assertEquals(List.of(ben.getId(), carla.getId()), friendIds(anna));
        session(carla, "Legs", Instant.now());
        assertEquals(List.of(carla.getId(), ben.getId()), friendIds(anna));
    }

    private User user(String name) {
        User user = new User(UUID.randomUUID().toString());
        user.setName(name);
        return userRepository.saveAndFlush(user);
    }

    private void befriend(User a, User b) {
        friendService.sendRequest(a.getId(), b.getId());
        friendService.sendRequest(b.getId(), a.getId());
    }

    private long session(User user, String name, Instant startedAt) {
        return jdbc.queryForObject("""
                INSERT INTO workout_session (user_id, name, started_at, log)
                VALUES (?, ?, ?, '{"exercises":[]}'::jsonb) RETURNING id
                """, Long.class, user.getId(), name, java.sql.Timestamp.from(startedAt));
    }

    private List<String> friendIds(User user) {
        return friendService.getFriends(user.getId()).friends().stream().map(FriendDto::id).toList();
    }

    private FriendDto friend(User user, User friend) {
        return friendService.getFriends(user.getId()).friends().stream()
                .filter((f) -> f.id().equals(friend.getId())).findFirst().orElseThrow();
    }

    private NotificationDto lastNotification(User user) {
        return notificationService.getNotifications(user.getId(), false, 1).getFirst();
    }
}

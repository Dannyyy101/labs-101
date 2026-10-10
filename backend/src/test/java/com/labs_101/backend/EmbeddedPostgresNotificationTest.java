package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.NotificationRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.NotificationService;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import(NotificationService.class)
public class EmbeddedPostgresNotificationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testCreateWithLink() {
        userRepository.save(new User("link-user"));
        notificationService.create("link-user", new CreateNotificationDto(NotificationType.SUCCESS,
                "Training beendet", "5 Übungen", "/workouts/3", "Ansehen"));
        flushAndClear();

        NotificationDto notification = notificationService.getNotifications("link-user", false, null).getFirst();
        assertEquals(NotificationType.SUCCESS, notification.getType());
        assertEquals("Training beendet", notification.getTitle());
        assertEquals("5 Übungen", notification.getMessage());
        assertEquals("/workouts/3", notification.getLink());
        assertEquals("Ansehen", notification.getLinkLabel());
        assertNotNull(notification.getCreatedAt());
        assertNull(notification.getReadAt());
    }

    @Test
    void testReadFlow() {
        userRepository.save(new User("read-user"));
        NotificationDto first = notificationService.create("read-user", new CreateNotificationDto("Erste"));
        notificationService.create("read-user", new CreateNotificationDto("Zweite"));
        notificationService.create("read-user", new CreateNotificationDto("Dritte"));
        flushAndClear();
        assertEquals(3, notificationService.countUnread("read-user"));

        notificationService.markRead("read-user", first.getId());
        flushAndClear();
        assertEquals(2, notificationService.countUnread("read-user"));
        List<String> unread = notificationService.getNotifications("read-user", true, null).stream()
                .map(NotificationDto::getTitle).toList();
        // newest first
        assertEquals(List.of("Dritte", "Zweite"), unread);

        assertEquals(2, notificationService.markAllRead("read-user"));
        flushAndClear();
        assertEquals(0, notificationService.countUnread("read-user"));
        assertEquals(3, notificationService.getNotifications("read-user", false, null).size());

        notificationService.markUnread("read-user", first.getId());
        flushAndClear();
        assertEquals(1, notificationService.countUnread("read-user"));
    }

    @Test
    void testOtherUsersNotificationsAreNotFound() {
        userRepository.save(new User("owner"));
        userRepository.save(new User("stranger"));
        NotificationDto notification = notificationService.create("owner", new CreateNotificationDto("Privat"));
        flushAndClear();

        assertThrows(NotFoundException.class, () -> notificationService.markRead("stranger", notification.getId()));
        assertThrows(NotFoundException.class, () -> notificationService.delete("stranger", notification.getId()));
        assertEquals(0, notificationService.markAllRead("stranger"));
        assertEquals(1, notificationService.countUnread("owner"));
    }

    @Test
    void testNotificationsAreDeletedWithUser() {
        userRepository.save(new User("deleted-user"));
        NotificationDto notification = notificationService.create("deleted-user", new CreateNotificationDto("Weg"));
        flushAndClear();

        // users are deleted in the database directly, the backend never deletes them
        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", "deleted-user");

        assertEquals(false, notificationRepository.existsById(notification.getId()));
    }
}

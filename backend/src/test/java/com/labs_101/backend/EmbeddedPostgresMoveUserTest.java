package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.UserSettings;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.UserSettingsRepository;
import com.labs_101.backend.services.NotificationService;

import jakarta.persistence.EntityManager;

/** move_user of V5, moves the data of a Better Auth user to their Zitadel id. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import(NotificationService.class)
public class EmbeddedPostgresMoveUserTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private void saveUser(String id, String email) {
        User user = new User(id);
        user.setName("Max");
        user.setEmail(email);
        userRepository.save(user);
    }

    private void moveUser(String oldId, String newId) {
        entityManager.flush();
        jdbc.queryForList("SELECT move_user(?, ?)", oldId, newId);
        entityManager.clear();
    }

    private int count(String sql, String userId) {
        return jdbc.queryForObject(sql, Integer.class, userId);
    }

    @Test
    void testMovesDataToNewUser() {
        saveUser("better-auth-id", "max@example.com");
        userSettingsRepository.save(new UserSettings(userRepository.getReferenceById("better-auth-id"), 2500));
        notificationService.create("better-auth-id", new CreateNotificationDto("Hallo"));

        moveUser("better-auth-id", "zitadel-id");

        assertFalse(userRepository.existsById("better-auth-id"));
        User moved = userRepository.findById("zitadel-id").orElseThrow();
        assertEquals("Max", moved.getName());
        assertEquals("max@example.com", moved.getEmail());
        assertEquals(2500, userSettingsRepository.findById("zitadel-id").orElseThrow().getCalorieGoal());
        assertEquals(1, notificationService.countUnread("zitadel-id"));
    }

    @Test
    void testMovesDataToUserThatAlreadySignedIn() {
        saveUser("better-auth-id", "max@example.com");
        // created by UserProvisioning with the same email before the move
        saveUser("zitadel-id", "max@example.com");
        notificationService.create("better-auth-id", new CreateNotificationDto("Hallo"));

        moveUser("better-auth-id", "zitadel-id");

        assertFalse(userRepository.existsById("better-auth-id"));
        assertEquals(1, notificationService.countUnread("zitadel-id"));
        assertEquals(0, count("SELECT count(*) FROM notification WHERE user_id = ?", "better-auth-id"));
    }

    @Test
    void testFailsForUnknownUser() {
        assertThrows(DataAccessException.class, () -> moveUser("unknown-id", "zitadel-id"));
    }
}

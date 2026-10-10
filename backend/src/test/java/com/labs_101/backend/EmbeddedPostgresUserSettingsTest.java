package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.settings.SaveUserSettingsDto;
import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.UserSettings;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.UserSettingsRepository;
import com.labs_101.backend.services.UserSettingsService;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import(UserSettingsService.class)
public class EmbeddedPostgresUserSettingsTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private UserSettingsService userSettingsService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // writes everything to the database and forgets the loaded entities,
    // so the next read really comes from postgres
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testSaveSettingsForUser() {
        userRepository.save(new User("settings-user"));
        userSettingsRepository.save(new UserSettings(userRepository.getReferenceById("settings-user"), 2500));
        flushAndClear();

        UserSettings settings = userSettingsRepository.findById("settings-user").orElseThrow();
        assertEquals("settings-user", settings.getUserId());
        assertEquals(2500, settings.getCalorieGoal());
    }

    @Test
    void testSettingsAreDeletedWithUser() {
        userRepository.save(new User("deleted-user"));
        userSettingsRepository.save(new UserSettings(userRepository.getReferenceById("deleted-user"), 2500));
        flushAndClear();

        // users are deleted in the database directly, the backend never deletes them
        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", "deleted-user");

        assertFalse(userSettingsRepository.existsById("deleted-user"));
    }

    @Test
    void testDeletingSettingsKeepsUser() {
        userRepository.save(new User("kept-user"));
        userSettingsService.createSettings("kept-user", new SaveUserSettingsDto(2500));
        flushAndClear();

        userSettingsService.deleteSettings("kept-user");
        flushAndClear();

        assertFalse(userSettingsRepository.existsById("kept-user"));
        assertEquals("kept-user", userRepository.findById("kept-user").orElseThrow().getId());
    }

    @Test
    void testCrudFlow() {
        userRepository.save(new User("crud-user"));

        UserSettingsDto created = userSettingsService.createSettings("crud-user", new SaveUserSettingsDto(3000));
        flushAndClear();
        assertEquals("crud-user", created.getUserId());
        assertEquals(3000, userSettingsService.getSettings("crud-user").getCalorieGoal());

        userSettingsService.updateSettings("crud-user", new SaveUserSettingsDto(2200));
        flushAndClear();
        assertEquals(2200, userSettingsService.getSettings("crud-user").getCalorieGoal());

        // without a value the goal stays as it is
        userSettingsService.updateSettings("crud-user", new SaveUserSettingsDto(null));
        flushAndClear();
        assertEquals(2200, userSettingsService.getSettings("crud-user").getCalorieGoal());

        userSettingsService.deleteSettings("crud-user");
        flushAndClear();
        assertThrows(NotFoundException.class, () -> userSettingsService.getSettings("crud-user"));
    }

    @Test
    void testSettingsAreSeparatedPerUser() {
        userRepository.save(new User("user-a"));
        userRepository.save(new User("user-b"));
        userSettingsService.createSettings("user-a", new SaveUserSettingsDto(2000));
        userSettingsService.createSettings("user-b", new SaveUserSettingsDto(3500));
        flushAndClear();

        userSettingsService.updateSettings("user-a", new SaveUserSettingsDto(1800));
        flushAndClear();

        assertEquals(1800, userSettingsService.getSettings("user-a").getCalorieGoal());
        assertEquals(3500, userSettingsService.getSettings("user-b").getCalorieGoal());
    }

    @Test
    void testCreateSettingsTwice() {
        userRepository.save(new User("twice-user"));
        userSettingsService.createSettings("twice-user", new SaveUserSettingsDto(2500));
        flushAndClear();

        assertThrows(BadRequestException.class,
                () -> userSettingsService.createSettings("twice-user", new SaveUserSettingsDto(3000)));
        assertEquals(2500, userSettingsService.getSettings("twice-user").getCalorieGoal());
    }

    @Test
    void testCreateSettingsForMissingUser() {
        assertThrows(NotFoundException.class,
                () -> userSettingsService.createSettings("missing-user", new SaveUserSettingsDto(2500)));
        assertFalse(userSettingsRepository.existsById("missing-user"));
    }
}

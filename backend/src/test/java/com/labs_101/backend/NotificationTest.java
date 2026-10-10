package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.Notification;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.NotificationRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.NotificationService;

@ExtendWith(MockitoExtension.class)
public class NotificationTest {

    private static final String USER_ID = "user-1";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private void userExists() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User(USER_ID));
        when(notificationRepository.save(any(Notification.class))).thenAnswer((invocation) -> invocation.getArgument(0));
    }

    @Test
    void testCreateDefaultsToInfo() {
        userExists();

        NotificationDto notification = notificationService.create(USER_ID,
                new CreateNotificationDto(null, "  Hallo  ", " ", " ", "Öffnen"));

        assertEquals(NotificationType.INFO, notification.getType());
        assertEquals("Hallo", notification.getTitle());
        assertNull(notification.getMessage());
        assertNull(notification.getLink());
        // a label without a link has nothing to label
        assertNull(notification.getLinkLabel());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/workouts/3", "/foods/track?date=2026-10-10", "https://example.com/a", "http://localhost:3000" })
    void testAllowedLinks(String link) {
        userExists();

        NotificationDto notification = notificationService.create(USER_ID,
                new CreateNotificationDto(NotificationType.INFO, "Titel", null, link, null));

        assertEquals(link, notification.getLink());
    }

    @ParameterizedTest
    @ValueSource(strings = { "javascript:alert(1)", "//evil.example", "/\\evil.example", "data:text/html,hi", "workouts", "ftp://example.com" })
    void testRejectedLinks(String link) {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> notificationService.create(USER_ID,
                new CreateNotificationDto(NotificationType.INFO, "Titel", null, link, null)));
        verify(notificationRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void testTitleIsRequired(String title) {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> notificationService.create(USER_ID, new CreateNotificationDto(title)));
    }

    @Test
    void testCreateForMissingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> notificationService.create(USER_ID, new CreateNotificationDto("Titel")));
    }
}

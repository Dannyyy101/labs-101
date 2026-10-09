package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labs_101.backend.dtos.settings.SaveUserSettingsDto;
import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.UserSettings;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.UserSettingsRepository;
import com.labs_101.backend.services.UserSettingsService;

@ExtendWith(MockitoExtension.class)
public class UserSettingsTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private static UserSettings settings(Integer calorieGoal) {
        return new UserSettings(USER_ID, new User(USER_ID), calorieGoal);
    }

    private void saveReturnsArgument() {
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer((invocation) -> invocation.getArgument(0));
    }

    @Test
    void testGetSettings() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings(2500)));

        UserSettingsDto settings = userSettingsService.getSettings(USER_ID);

        assertEquals(USER_ID, settings.getUserId());
        assertEquals(2500, settings.getCalorieGoal());
    }

    @Test
    void testGetMissingSettings() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userSettingsService.getSettings(USER_ID));
    }

    @Test
    void testCreateSettings() {
        when(userSettingsRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User(USER_ID));
        saveReturnsArgument();

        UserSettingsDto settings = userSettingsService.createSettings(USER_ID, new SaveUserSettingsDto(2500));

        assertEquals(2500, settings.getCalorieGoal());
    }

    @Test
    void testCreateSettingsWithoutCalorieGoal() {
        when(userSettingsRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User(USER_ID));
        saveReturnsArgument();

        UserSettingsDto settings = userSettingsService.createSettings(USER_ID, new SaveUserSettingsDto(null));

        assertNull(settings.getCalorieGoal());
    }

    @Test
    void testCreateSettingsTwice() {
        when(userSettingsRepository.existsById(USER_ID)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userSettingsService.createSettings(USER_ID, new SaveUserSettingsDto(2500)));
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void testCreateSettingsForMissingUser() {
        when(userSettingsRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> userSettingsService.createSettings(USER_ID, new SaveUserSettingsDto(2500)));
        verify(userSettingsRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -2500 })
    void testCreateSettingsWithInvalidCalorieGoal(int calorieGoal) {
        when(userSettingsRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userSettingsService.createSettings(USER_ID, new SaveUserSettingsDto(calorieGoal)));
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void testUpdateSettings() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings(3000)));
        saveReturnsArgument();

        UserSettingsDto settings = userSettingsService.updateSettings(USER_ID, new SaveUserSettingsDto(2200));

        assertEquals(2200, settings.getCalorieGoal());
    }

    @Test
    void testUpdateSettingsWithoutCalorieGoalKeepsIt() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings(3000)));
        saveReturnsArgument();

        UserSettingsDto settings = userSettingsService.updateSettings(USER_ID, new SaveUserSettingsDto(null));

        assertEquals(3000, settings.getCalorieGoal());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void testUpdateSettingsWithInvalidCalorieGoal(int calorieGoal) {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings(3000)));

        assertThrows(BadRequestException.class,
                () -> userSettingsService.updateSettings(USER_ID, new SaveUserSettingsDto(calorieGoal)));
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void testUpdateMissingSettings() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userSettingsService.updateSettings(USER_ID, new SaveUserSettingsDto(2500)));
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void testDeleteSettings() {
        UserSettings settings = settings(3000);
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings));

        userSettingsService.deleteSettings(USER_ID);

        verify(userSettingsRepository).delete(settings);
    }

    @Test
    void testDeleteMissingSettings() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userSettingsService.deleteSettings(USER_ID));
        verify(userSettingsRepository, never()).delete(any());
    }
}

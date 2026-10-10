package com.labs_101.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Constructor;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.labs_101.backend.controller.UserSettingsController;
import com.labs_101.backend.dtos.settings.SaveUserSettingsDto;
import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.global.GlobalExceptionHandler;
import com.labs_101.backend.services.UserSettingsService;

@ExtendWith(MockitoExtension.class)
public class UserSettingsControllerTest {

    private static final String USER_ID = "user-1";
    private static final String URL = "/api/users/me/settings";

    @Mock
    private UserSettingsService userSettingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        // the constructor is package private like in all controllers
        Constructor<UserSettingsController> constructor = UserSettingsController.class
                .getDeclaredConstructor(UserSettingsService.class);
        constructor.setAccessible(true);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        mockMvc = MockMvcBuilders.standaloneSetup(constructor.newInstance(userSettingsService))
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        // signed in as USER_ID, like the token check would leave it
        Jwt token = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static boolean hasCalorieGoal(SaveUserSettingsDto dto, Integer calorieGoal) {
        return calorieGoal.equals(dto.getCalorieGoal());
    }

    @Test
    void testGetSettings() throws Exception {
        when(userSettingsService.getSettings(USER_ID)).thenReturn(new UserSettingsDto(USER_ID, 2500));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.calorieGoal").value(2500));
    }

    @Test
    void testGetMissingSettings() throws Exception {
        when(userSettingsService.getSettings(USER_ID)).thenThrow(NotFoundException.userSettings(USER_ID));

        mockMvc.perform(get(URL).locale(Locale.ENGLISH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorMessage").value("Settings for user user-1 not found"))
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    void testGetMissingSettingsInGerman() throws Exception {
        when(userSettingsService.getSettings(USER_ID)).thenThrow(NotFoundException.userSettings(USER_ID));

        mockMvc.perform(get(URL).locale(Locale.GERMAN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Einstellungen für Benutzer user-1 nicht gefunden"));
    }

    @Test
    void testCreateSettings() throws Exception {
        when(userSettingsService.createSettings(eq(USER_ID), argThat((dto) -> hasCalorieGoal(dto, 2500))))
                .thenReturn(new UserSettingsDto(USER_ID, 2500));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"calorieGoal\": 2500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.calorieGoal").value(2500));
    }

    @Test
    void testCreateExistingSettings() throws Exception {
        when(userSettingsService.createSettings(eq(USER_ID), any()))
                .thenThrow(BadRequestException.userSettingsExist(USER_ID));

        mockMvc.perform(post(URL).locale(Locale.ENGLISH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calorieGoal\": 2500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Settings for user user-1 already exist"));
    }

    @Test
    void testCreateSettingsForMissingUser() throws Exception {
        when(userSettingsService.createSettings(eq(USER_ID), any())).thenThrow(NotFoundException.user(USER_ID));

        mockMvc.perform(post(URL).locale(Locale.ENGLISH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calorieGoal\": 2500}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User user-1 not found"));
    }

    @Test
    void testUpdateSettings() throws Exception {
        when(userSettingsService.updateSettings(eq(USER_ID), argThat((dto) -> hasCalorieGoal(dto, 2200))))
                .thenReturn(new UserSettingsDto(USER_ID, 2200));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"calorieGoal\": 2200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calorieGoal").value(2200));
    }

    @Test
    void testUpdateSettingsWithInvalidCalorieGoal() throws Exception {
        when(userSettingsService.updateSettings(eq(USER_ID), any()))
                .thenThrow(BadRequestException.calorieGoal(0));

        mockMvc.perform(put(URL).locale(Locale.ENGLISH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calorieGoal\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorMessage").value("Calorie goal must be greater than 0, but was 0"));
    }

    @Test
    void testUpdateMissingSettings() throws Exception {
        when(userSettingsService.updateSettings(eq(USER_ID), any()))
                .thenThrow(NotFoundException.userSettings(USER_ID));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"calorieGoal\": 2200}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteSettings() throws Exception {
        mockMvc.perform(delete(URL))
                .andExpect(status().isNoContent());

        verify(userSettingsService).deleteSettings(USER_ID);
    }

    @Test
    void testDeleteMissingSettings() throws Exception {
        doThrow(NotFoundException.userSettings(USER_ID)).when(userSettingsService).deleteSettings(USER_ID);

        mockMvc.perform(delete(URL))
                .andExpect(status().isNotFound());
    }
}

package com.labs_101.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.settings.SaveUserSettingsDto;
import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.services.UserSettingsService;

@RestController()
@RequestMapping("/api/users/{userId}/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping("")
    public UserSettingsDto getSettings(@PathVariable String userId) {
        return userSettingsService.getSettings(userId);
    }

    @PostMapping("")
    public ResponseEntity<UserSettingsDto> createSettings(@PathVariable String userId,
            @RequestBody SaveUserSettingsDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userSettingsService.createSettings(userId, dto));
    }

    @PutMapping("")
    public UserSettingsDto updateSettings(@PathVariable String userId, @RequestBody SaveUserSettingsDto dto) {
        return userSettingsService.updateSettings(userId, dto);
    }

    @DeleteMapping("")
    public ResponseEntity<Void> deleteSettings(@PathVariable String userId) {
        userSettingsService.deleteSettings(userId);
        return ResponseEntity.noContent().build();
    }
}

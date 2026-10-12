package com.labs_101.backend.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.dtos.profile.ProfileDto;
import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.security.CurrentUser;
import com.labs_101.backend.services.ProfileService;

/**
 * Login streak, activity heatmap and achievements of the profile page. Not
 * called ProfileController, Spring Data REST already has a bean of that name.
 */
@RestController()
@RequestMapping("/api/users/me/profile")
public class UserProfileController {

    private final ProfileService profileService;

    UserProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** {@code zone} defaults to Europe/Berlin, the zone the login days are counted in. */
    @GetMapping
    public ProfileDto getProfile(@CurrentUser String userId, @RequestParam(required = false) String zone) {
        ZoneId zoneId;
        try {
            zoneId = zone != null ? ZoneId.of(zone) : LoginDayRepository.ZONE;
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown time zone " + zone);
        }
        return profileService.getProfile(userId, LocalDate.now(zoneId), zoneId);
    }
}

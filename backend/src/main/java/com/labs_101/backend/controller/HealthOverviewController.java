package com.labs_101.backend.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.dtos.overview.HealthOverviewDto;
import com.labs_101.backend.services.HealthOverviewService;
import com.labs_101.backend.security.CurrentUser;

/** Sleep, recovery and strain of a day for the home page, see {@link HealthController}. */
@RestController()
@RequestMapping("/api/users/me/health/overview")
public class HealthOverviewController {

    private final HealthOverviewService healthOverviewService;

    HealthOverviewController(HealthOverviewService healthOverviewService) {
        this.healthOverviewService = healthOverviewService;
    }

    /** {@code date} defaults to today, {@code zone} to Europe/Berlin. */
    @GetMapping
    public HealthOverviewDto getOverview(@CurrentUser String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String zone) {
        ZoneId zoneId;
        try {
            zoneId = zone != null ? ZoneId.of(zone) : HealthOverviewService.DEFAULT_ZONE;
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown time zone " + zone);
        }
        return healthOverviewService.getOverview(userId, date != null ? date : LocalDate.now(zoneId), zoneId);
    }
}

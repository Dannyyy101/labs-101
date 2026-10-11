package com.labs_101.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.calendar.ConnectAppleAccountDto;
import com.labs_101.backend.dtos.calendar.CreateExternalCalendarDto;
import com.labs_101.backend.dtos.calendar.ExternalCalendarDto;
import com.labs_101.backend.dtos.calendar.ExternalCalendarEventDto;
import com.labs_101.backend.security.CurrentUser;
import com.labs_101.backend.services.ExternalCalendarService;

/** Calendars of other apps the signed in user shows in their calendar, read only. */
@RestController()
@RequestMapping("/api/calendar/external")
public class ExternalCalendarController {
    private final ExternalCalendarService externalCalendarService;

    ExternalCalendarController(ExternalCalendarService externalCalendarService) {
        this.externalCalendarService = externalCalendarService;
    }

    @GetMapping("")
    public List<ExternalCalendarDto> getCalendars(@CurrentUser String userId) {
        return externalCalendarService.getCalendars(userId);
    }

    @PostMapping("")
    public ResponseEntity<ExternalCalendarDto> addCalendar(@CurrentUser String userId,
            @RequestBody CreateExternalCalendarDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(externalCalendarService.addCalendar(userId, dto));
    }

    /** Signs in with the Apple ID and an app-specific password, all calendars of the user afterwards. */
    @PostMapping("/apple")
    public List<ExternalCalendarDto> connectAppleAccount(@CurrentUser String userId,
            @RequestBody ConnectAppleAccountDto dto) {
        return externalCalendarService.connectAppleAccount(userId, dto);
    }

    @DeleteMapping("/apple/{accountId}")
    public ResponseEntity<Void> disconnectAppleAccount(@CurrentUser String userId, @PathVariable Long accountId) {
        externalCalendarService.disconnectAppleAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalendar(@CurrentUser String userId, @PathVariable Long id) {
        externalCalendarService.deleteCalendar(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public List<ExternalCalendarEventDto> getEvents(@CurrentUser String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        return externalCalendarService.getEvents(userId, startDate, endDate);
    }
}

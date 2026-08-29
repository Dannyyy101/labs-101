package com.labs_101.backend.controller;

import com.labs_101.backend.services.CalendarService;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;

@RestController()
@RequestMapping("/api/calendar")
public class CalendarController {
  private final CalendarService calendarService;

  CalendarController(CalendarService calendarService) {
    this.calendarService = calendarService;
  }

  @GetMapping("")
  public List<CalendarEventDto> getAllCalendarEvents(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
    try {
      return calendarService.getAllCalendarEvents(startDate, endDate);
    } catch (Exception e) {
      throw e;
    }
  }

  @PostMapping("")
  public ResponseEntity<Void> createCalendarEvent(@RequestBody CreateCalendarEventDto eventDto) {
    try {
      calendarService.createCalendarEvent(eventDto);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  @PutMapping("/{id}")
  public CalendarEventDto updateCalendarEvent(@PathVariable Long id, @RequestBody UpdateCalendarEventDto eventDto) {
    try {
      if (!id.equals(eventDto.getId())) {
        throw new Error();
      }
      return calendarService.updateCalendarEvent(eventDto);
    } catch (Exception e) {
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCalendarEvent(@PathVariable Long id) {
    try {
      calendarService.deleteCalendarEvent(id);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      throw e;
    }
  }
}

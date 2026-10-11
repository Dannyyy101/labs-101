package com.labs_101.backend.dtos.calendar;

import java.time.Instant;
import java.util.List;

import com.labs_101.backend.entities.Recurrence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CalendarEventDto {
    private Long id;
    private String title;
    private Instant startDate;
    private Instant endDate;
    private String creatorId;
    private Long workoutId;
    private String workoutName;
    private CalendarUserDto creator;
    private List<CalendarUserDto> invitees;
    private Recurrence recurrence;
    private Instant recurrenceEnd;
}

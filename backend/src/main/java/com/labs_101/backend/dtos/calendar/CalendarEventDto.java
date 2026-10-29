package com.labs_101.backend.dtos.calendar;

import org.jspecify.annotations.Nullable;

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
    private @Nullable Long workoutId;
    private @Nullable String workoutName;
    private @Nullable CalendarUserDto creator;
    private List<CalendarUserDto> invitees;
    private @Nullable Recurrence recurrence;
    private @Nullable Instant recurrenceEnd;
}

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
public class CreateCalendarEventDto {
    private String title;
    private Instant startDate;
    private Instant endDate;
    private @Nullable String creatorId;
    private @Nullable Long workoutId;
    private @Nullable List<String> inviteeIds;
    private @Nullable Recurrence recurrence;
    private @Nullable Instant recurrenceEnd;
}

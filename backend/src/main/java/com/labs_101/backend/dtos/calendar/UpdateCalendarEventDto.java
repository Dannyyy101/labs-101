package com.labs_101.backend.dtos.calendar;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UpdateCalendarEventDto {
    private Long id;
    private String title;
    private Instant startDate;
    private Instant endDate;
    /** Always applied, null removes the planned workout. */
    private Long workoutId;
    /** Replaces the invitees, null keeps them. */
    private List<String> inviteeIds;
}

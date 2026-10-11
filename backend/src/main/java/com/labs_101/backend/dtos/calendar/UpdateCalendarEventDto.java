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
public class UpdateCalendarEventDto {
    private Long id;
    private String title;
    private Instant startDate;
    private Instant endDate;
    /** Always applied, null removes the planned workout. */
    private Long workoutId;
    /** Replaces the invitees, null keeps them. */
    private List<String> inviteeIds;
    /** Always applied, null ends the repetition. */
    private Recurrence recurrence;
    /** Always applied, null repeats forever. */
    private Instant recurrenceEnd;
    /**
     * The start of the edited occurrence of a repeating event. The series is moved by as much as this
     * occurrence was, null when the first one or a single event was edited.
     */
    private Instant occurrenceStartDate;
}

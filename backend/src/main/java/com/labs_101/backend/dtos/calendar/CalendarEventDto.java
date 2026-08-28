package com.labs_101.backend.dtos.calendar;

import java.time.Instant;

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
}

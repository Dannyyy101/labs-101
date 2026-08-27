package com.labs_101.backend.dtos.calendar;

import java.time.Instant;

import com.labs_101.backend.dtos.workout.CreateWorkoutDto;

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
    private CreateWorkoutDto workoutDto;
}

package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.entities.CalendarEvent;
import com.labs_101.backend.entities.workout.Workout;

public class CalendarMapper {
    public static CalendarEvent fromCreateCalendarEventDto(CreateCalendarEventDto dto, Workout workoutDto) {
        return new CalendarEvent(null, dto.getTitle(), dto.getStartDate(), dto.getEndDate(), workoutDto);
    }

    public static CalendarEventDto fromCalendarEvent(CalendarEvent event){
        return new CalendarEventDto(event.getId(), event.getTitle(), event.getStartDate(), event.getEndDate());
    }
}

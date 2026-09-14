package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.entities.CalendarEvent;
import com.labs_101.backend.entities.User;

public class CalendarMapper {
    public static CalendarEvent fromCreateCalendarEventDto(CreateCalendarEventDto dto) {
        return new CalendarEvent(null, dto.getTitle(), dto.getStartDate(), dto.getEndDate(),
                new User(dto.getCreatorId()));
    }

    public static CalendarEventDto fromCalendarEvent(CalendarEvent event) {
        return new CalendarEventDto(event.getId(), event.getTitle(), event.getStartDate(), event.getEndDate(),
                event.getCreator().getId());
    }
}

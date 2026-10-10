package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.entities.CalendarEvent;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.workout.Workout;

import java.util.Comparator;
import java.util.LinkedHashSet;

public class CalendarMapper {
    public static CalendarEvent fromCreateCalendarEventDto(CreateCalendarEventDto dto, User creator, Workout workout) {
        return new CalendarEvent(null, dto.getTitle(), dto.getStartDate(), dto.getEndDate(), creator, workout,
                new LinkedHashSet<>());
    }

    public static CalendarEventDto fromCalendarEvent(CalendarEvent event) {
        Workout workout = event.getWorkout();
        return new CalendarEventDto(event.getId(), event.getTitle(), event.getStartDate(), event.getEndDate(),
                event.getCreator().getId(), workout == null ? null : workout.getId(),
                workout == null ? null : workout.getName(), fromUser(event.getCreator()),
                // sorted, the stored set has no order
                event.getInvitees().stream().map(CalendarMapper::fromUser)
                        .sorted(Comparator.comparing(CalendarUserDto::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList());
    }

    public static CalendarUserDto fromUser(User user) {
        return new CalendarUserDto(user.getId(), user.getName(), user.getImage());
    }
}

package com.labs_101.backend.services;

import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.CalendarRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;
import com.labs_101.backend.dtos.workout.WorkoutHeaderDto;
import com.labs_101.backend.entities.CalendarEvent;

@Service
public class CalendarService {

    private final WorkoutService workoutService;
    private final CalendarRepository calendarRepository;

    CalendarService(CalendarRepository calendarRepository, WorkoutService workoutService) {
        this.calendarRepository = calendarRepository;
        this.workoutService = workoutService;
    }

    public void createCalendarEvent(CreateCalendarEventDto eventDto) {
        calendarRepository.save(CalendarMapper.fromCreateCalendarEventDto(eventDto));
    }

    public List<CalendarEventDto> getAllCalendarEvents(Instant startDate, Instant endDate) {
        if (startDate == null && endDate == null) {
            return calendarRepository.findAll().stream().map((event) -> CalendarMapper.fromCalendarEvent(event))
                    .toList();
        }
        return calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate).stream()
                .map((event) -> CalendarMapper.fromCalendarEvent(event)).toList();
    }

    public void deleteCalendarEvent(Long id) {
        calendarRepository.deleteById(id);
    }

    public CalendarEventDto updateCalendarEvent(UpdateCalendarEventDto eventDto) {
        CalendarEvent event = calendarRepository.findById(eventDto.getId()).orElseThrow();

        if (eventDto.getTitle() != null) {
            event.setTitle(eventDto.getTitle());
        }

        if (eventDto.getStartDate() != null) {
            event.setStartDate(eventDto.getStartDate());
        }

        if (eventDto.getEndDate() != null) {
            event.setEndDate(eventDto.getEndDate());
        }

        CalendarEvent updatedCalendarEvent = calendarRepository.save(event);

        return CalendarMapper.fromCalendarEvent(updatedCalendarEvent);
    }

}

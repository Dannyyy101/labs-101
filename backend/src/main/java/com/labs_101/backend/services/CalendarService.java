package com.labs_101.backend.services;

import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.CalendarRepository;

import java.util.List;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;

@Service
public class CalendarService {
    
    private final CalendarRepository calendarRepository;

    CalendarService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    public void createCalendarEvent(CreateCalendarEventDto eventDto) {
        calendarRepository.save(CalendarMapper.fromCreateCalendarEventDto(eventDto));
    }

    public List<CalendarEventDto> getAllCalendarEvents() {
        return calendarRepository.findAll().stream().map((event) -> CalendarMapper.fromCalendarEvent(event)).toList();
    }

    public void deleteCalendarEvent(String id) {
        calendarRepository.deleteById(id);
    }

  
}

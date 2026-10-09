package com.labs_101.backend.services;

import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.CalendarRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;
import com.labs_101.backend.entities.CalendarEvent;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.exception.NotFoundException;

@Service
public class CalendarService {

    // an open window, Instant.MIN/MAX don't fit in a postgres timestamp
    private static final Instant EARLIEST = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T23:59:59Z");

    private final WorkoutRepository workoutRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    CalendarService(CalendarRepository calendarRepository, WorkoutRepository workoutRepository,
            UserRepository userRepository, NotificationService notificationService) {
        this.calendarRepository = calendarRepository;
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CalendarEventDto createCalendarEvent(CreateCalendarEventDto eventDto) {
        User creator = findUser(eventDto.getCreatorId());
        CalendarEvent event = CalendarMapper.fromCreateCalendarEventDto(eventDto, creator,
                findWorkout(eventDto.getWorkoutId()));
        event.setInvitees(findInvitees(eventDto.getInviteeIds(), creator));
        CalendarEvent saved = calendarRepository.save(event);
        notifyInvitees(saved, saved.getInvitees());
        return CalendarMapper.fromCalendarEvent(saved);
    }

    /** All events without a user, otherwise the ones the user created or was invited to. */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getAllCalendarEvents(String userId, Instant startDate, Instant endDate) {
        List<CalendarEvent> events;
        if (userId != null) {
            events = calendarRepository.findForUser(userId, startDate == null ? EARLIEST : startDate,
                    endDate == null ? LATEST : endDate);
        } else if (startDate == null && endDate == null) {
            events = calendarRepository.findAll();
        } else {
            events = calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate);
        }
        return events.stream().map(CalendarMapper::fromCalendarEvent).toList();
    }

    public void deleteCalendarEvent(Long id) {
        calendarRepository.deleteById(id);
    }

    @Transactional
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

        event.setWorkout(findWorkout(eventDto.getWorkoutId()));

        Set<User> added = Set.of();
        if (eventDto.getInviteeIds() != null) {
            Set<User> invitees = findInvitees(eventDto.getInviteeIds(), event.getCreator());
            added = new LinkedHashSet<>(invitees);
            added.removeAll(event.getInvitees());
            event.getInvitees().clear();
            event.getInvitees().addAll(invitees);
        }

        CalendarEvent updatedCalendarEvent = calendarRepository.save(event);
        // only the newly invited, the others already know about the event
        notifyInvitees(updatedCalendarEvent, added);

        return CalendarMapper.fromCalendarEvent(updatedCalendarEvent);
    }

    private Workout findWorkout(Long workoutId) {
        if (workoutId == null) {
            return null;
        }
        return workoutRepository.findById(workoutId).orElseThrow(() -> NotFoundException.workout(workoutId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> NotFoundException.user(userId));
    }

    /** The creator can't invite themselves, duplicates are dropped. */
    private Set<User> findInvitees(List<String> inviteeIds, User creator) {
        Set<User> invitees = new LinkedHashSet<>();
        if (inviteeIds == null) {
            return invitees;
        }
        for (String id : inviteeIds) {
            if (!id.equals(creator.getId())) {
                invitees.add(findUser(id));
            }
        }
        return invitees;
    }

    private void notifyInvitees(CalendarEvent event, Set<User> invitees) {
        String creatorName = event.getCreator().getName() == null ? "Jemand" : event.getCreator().getName();
        String title = event.getTitle() == null || event.getTitle().isBlank() ? "einem Termin" : "„" + event.getTitle() + "“";
        for (User invitee : invitees) {
            notificationService.notify(invitee.getId(), NotificationType.INFO, "Neue Einladung",
                    creatorName + " hat dich zu " + title + " eingeladen.", "/planner");
        }
    }
}

package com.labs_101.backend.services;

import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.CalendarRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.calendar.UpdateCalendarEventDto;
import com.labs_101.backend.entities.CalendarEvent;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.Recurrence;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.security.ZitadelClient;

@Service
public class CalendarService {

    // an open window, Instant.MIN/MAX don't fit in a postgres timestamp
    private static final Instant EARLIEST = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T23:59:59Z");
    // repeating events keep their local time across daylight saving time, the users are in germany
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    /** "Mo., 13.10. um 18:30" in the invite, it is also shown as a push on the phone */
    private static final DateTimeFormatter INVITE_DATE = DateTimeFormatter.ofPattern("EE, dd.MM. 'um' HH:mm", Locale.GERMAN);
    // a daily event in a window of several years, more than any calendar view shows
    private static final int MAX_OCCURRENCES = 1000;

    private final WorkoutRepository workoutRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ZitadelClient zitadelClient;

    CalendarService(CalendarRepository calendarRepository, WorkoutRepository workoutRepository,
            UserRepository userRepository, NotificationService notificationService, ZitadelClient zitadelClient) {
        this.zitadelClient = zitadelClient;
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
        return toDtos(List.of(saved)).getFirst();
    }

    /**
     * All events without a user, otherwise the ones the user created or was invited to. With both dates a
     * repeating event is listed once per occurrence in the window, without them only its first occurrence.
     */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getAllCalendarEvents(String userId, Instant startDate, Instant endDate) {
        List<CalendarEvent> events;
        if (userId != null) {
            events = calendarRepository.findForUser(userId, startDate == null ? EARLIEST : startDate,
                    endDate == null ? LATEST : endDate);
        } else if (startDate == null && endDate == null) {
            events = calendarRepository.findAll();
        } else {
            events = calendarRepository.findInWindow(startDate == null ? EARLIEST : startDate,
                    endDate == null ? LATEST : endDate);
        }
        if (startDate == null || endDate == null) {
            return toDtos(events);
        }
        return events.stream().flatMap((event) -> occurrences(event, startDate, endDate).stream())
                .sorted(Comparator.comparing(CalendarEventDto::getStartDate)).toList();
    }

    /** The occurrences of the event that overlap the window, a single event has at most one. */
    private List<CalendarEventDto> occurrences(CalendarEvent event, Instant windowStart, Instant windowEnd) {
        Duration duration = Duration.between(event.getStartDate(), event.getEndDate());
        List<CalendarEventDto> occurrences = new ArrayList<>();
        if (event.getRecurrence() == null) {
            if (!event.getStartDate().isAfter(windowEnd) && !event.getEndDate().isBefore(windowStart)) {
                occurrences.add(toDto(event, event.getStartDate(), event.getEndDate()));
            }
            return occurrences;
        }

        ChronoUnit unit = unit(event.getRecurrence());
        ZonedDateTime first = event.getStartDate().atZone(ZONE);
        // skip the occurrences that end before the window, one less as the units don't have the same length
        long n = Math.max(0, unit.between(first, windowStart.minus(duration).atZone(ZONE)) - 1);
        for (int count = 0; count < MAX_OCCURRENCES; n++) {
            // always from the first, a monthly event on the 31st is on the 30th in april but on the 31st in may again
            Instant start = first.plus(n, unit).toInstant();
            if (start.isAfter(windowEnd)
                    || (event.getRecurrenceEnd() != null && start.isAfter(event.getRecurrenceEnd()))) {
                break;
            }
            Instant end = start.plus(duration);
            if (!end.isBefore(windowStart)) {
                occurrences.add(toDto(event, start, end));
                count++;
            }
        }
        return occurrences;
    }

    private static ChronoUnit unit(Recurrence recurrence) {
        return switch (recurrence) {
            case DAILY -> ChronoUnit.DAYS;
            case WEEKLY -> ChronoUnit.WEEKS;
            case MONTHLY -> ChronoUnit.MONTHS;
        };
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

        Instant startDate = eventDto.getStartDate() == null ? event.getStartDate() : eventDto.getStartDate();
        Instant endDate = eventDto.getEndDate() == null ? event.getEndDate() : eventDto.getEndDate();
        if (eventDto.getOccurrenceStartDate() != null && event.getRecurrence() != null) {
            // the dates are the edited occurrence's, the series is moved by as much as it was
            Duration duration = Duration.between(startDate, endDate);
            startDate = event.getStartDate().plus(Duration.between(eventDto.getOccurrenceStartDate(), startDate));
            endDate = startDate.plus(duration);
        }
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setRecurrence(eventDto.getRecurrence());
        event.setRecurrenceEnd(eventDto.getRecurrenceEnd());

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

        return toDtos(List.of(updatedCalendarEvent)).getFirst();
    }

    private List<CalendarEventDto> toDtos(List<CalendarEvent> events) {
        return events.stream().map((event) -> CalendarMapper.fromCalendarEvent(event, zitadelClient::avatarUrl)).toList();
    }

    private CalendarEventDto toDto(CalendarEvent event, Instant startDate, Instant endDate) {
        return CalendarMapper.fromCalendarEvent(event, startDate, endDate, zitadelClient::avatarUrl);
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
        String when = event.getStartDate() == null ? "" : " am " + INVITE_DATE.format(event.getStartDate().atZone(ZONE));
        for (User invitee : invitees) {
            notificationService.notify(invitee.getId(), NotificationType.INFO, "Neue Einladung",
                    creatorName + " hat dich zu " + title + when + " eingeladen.", "/planner");
        }
    }
}

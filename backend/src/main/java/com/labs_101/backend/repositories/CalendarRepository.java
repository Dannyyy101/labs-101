package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.CalendarEvent;

public interface CalendarRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(Instant windowEnd, Instant windowStart);
}

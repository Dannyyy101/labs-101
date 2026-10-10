package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.CalendarEvent;

public interface CalendarRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(Instant windowEnd, Instant windowStart);

    List<CalendarEvent> findByWorkout_Id(Long workoutId);

    /** Events the user created or was invited to that overlap the window. */
    @Query("""
            select distinct e from CalendarEvent e left join e.invitees i
            where (e.creator.id = :userId or i.id = :userId)
            and e.startDate <= :windowEnd and e.endDate >= :windowStart
            """)
    List<CalendarEvent> findForUser(@Param("userId") String userId, @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}

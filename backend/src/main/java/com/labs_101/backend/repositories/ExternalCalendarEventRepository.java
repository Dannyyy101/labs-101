package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.ExternalCalendarEvent;

public interface ExternalCalendarEventRepository extends JpaRepository<ExternalCalendarEvent, Long> {
    /** The window includes its edges like {@link com.labs_101.backend.calendarImport.IcsParser#events}. */
    @Query("""
            select e from ExternalCalendarEvent e join fetch e.calendar c
            where c.user.id = :userId and e.startDate <= :end and e.endDate >= :start
            order by e.startDate""")
    List<ExternalCalendarEvent> findInWindow(@Param("userId") String userId, @Param("start") Instant start,
            @Param("end") Instant end);

    @Modifying
    @Query("""
            delete from ExternalCalendarEvent e
            where e.calendar.id = :calendarId and e.startDate <= :end and e.endDate >= :start""")
    void deleteInWindow(@Param("calendarId") Long calendarId, @Param("start") Instant start,
            @Param("end") Instant end);
}

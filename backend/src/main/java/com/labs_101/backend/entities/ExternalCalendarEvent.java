package com.labs_101.backend.entities;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One occurrence of an event of an {@link ExternalCalendar} as it was loaded the last time. The calendar page shows
 * these right away, loading from Apple takes seconds.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "external_calendar_event", indexes = @Index(columnList = "calendar_id, start_date"))
public class ExternalCalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExternalCalendar calendar;

    @Column(length = 1024)
    private String uid;

    @Column(length = 2048)
    private String title;

    private Instant startDate;

    private Instant endDate;

    private boolean allDay;
}

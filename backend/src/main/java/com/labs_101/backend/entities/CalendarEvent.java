package com.labs_101.backend.entities;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Document
public class CalendarEvent {
    private @Id String id;
    private String title;
    private Instant startDate;
    private Instant endDate;
    @DBRef
    private Workout workout;
}

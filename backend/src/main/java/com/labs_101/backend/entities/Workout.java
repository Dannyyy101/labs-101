package com.labs_101.backend.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Document
public class Workout {
    private @Id String id;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String workoutTemplateId;
}

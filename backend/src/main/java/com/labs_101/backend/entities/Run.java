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
@Document("runs")
public class Run {
    @Id
	private String uuid;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long duration;
}

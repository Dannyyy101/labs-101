package com.labs_101.backend.entities.workoutTemplate;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Document("workout_templates")
public class WorkoutTemplate {
    @Id
    private String id;
    private String name;
    private List<PlanItem> items;
}




package com.labs_101.backend.entities.workoutTemplate;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@TypeAlias("run")
public record RunItem(String exerciseId, int order, String type) implements PlanItem {
    @PersistenceCreator
    public RunItem {
    }
}

package com.labs_101.backend.entities.workoutTemplate;

import java.lang.annotation.Documented;
import java.util.List;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@TypeAlias("strength")
public record StrengthItem(String exerciseId, int order, String type, List<PlannedSet> sets) implements PlanItem {
    public StrengthItem {
    }
}

package com.labs_101.backend.entities.workoutTemplate;


public sealed interface PlanItem permits StrengthItem, RunItem {
    String exerciseId();
    int order();
    String type();
}

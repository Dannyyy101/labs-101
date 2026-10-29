package com.labs_101.backend.dtos.profile;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The profile page: login streak, activity of the last year and achievements.
 *
 * @param days        days of the last 53 weeks with any activity, oldest first
 * @param calorieGoal kcal per day from the settings, null without one
 */
public record ProfileDto(@Nullable Instant memberSince, LocalDate today, StreakDto streak, List<ActivityDayDto> days,
        List<AchievementDto> achievements, @Nullable Integer calorieGoal) {
}

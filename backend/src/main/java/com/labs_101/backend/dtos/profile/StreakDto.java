package com.labs_101.backend.dtos.profile;

/**
 * Days in a row the user used the app. The current streak still counts while
 * today is missing, it only breaks after a whole day without the app.
 */
public record StreakDto(int current, int record, boolean today) {
}

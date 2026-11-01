package com.labs_101.backend.dtos.workout;

import java.util.List;

/** @param userIds the friends the template is shared with, replaces the ones before */
public record ShareWorkoutDto(List<String> userIds) {
}

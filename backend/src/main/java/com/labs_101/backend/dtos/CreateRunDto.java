package com.labs_101.backend.dtos;

import java.time.LocalDateTime;

public record CreateRunDto(String uuid,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long duration) {
}

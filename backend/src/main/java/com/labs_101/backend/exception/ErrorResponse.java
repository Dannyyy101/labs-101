package com.labs_101.backend.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorMessage,
        String path) {
}
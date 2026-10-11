package com.labs_101.backend.dtos.calendar;

/** {@code password} is an app-specific password, not the one of the Apple ID. */
public record ConnectAppleAccountDto(String appleId, String password) {
}

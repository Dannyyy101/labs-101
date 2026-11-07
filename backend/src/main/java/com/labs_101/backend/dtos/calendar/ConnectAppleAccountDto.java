package com.labs_101.backend.dtos.calendar;

/** {@code password} is an app-specific password, not the one of the Apple ID. */
public record ConnectAppleAccountDto(String appleId, String password) {

    // spring logs the bodies it reads, the password must not end up in the log
    @Override
    public String toString() {
        return "ConnectAppleAccountDto[appleId=" + appleId + ", password=***]";
    }
}

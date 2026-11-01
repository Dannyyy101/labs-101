package com.labs_101.backend.dtos.user;

import org.jspecify.annotations.Nullable;

/** The signed in user, as synced from Zitadel. {@code image} is the profile picture, null without one. */
public record UserProfileDto(String id, @Nullable String name, @Nullable String email, @Nullable String image) {
}

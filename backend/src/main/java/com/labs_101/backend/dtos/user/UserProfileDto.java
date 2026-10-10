package com.labs_101.backend.dtos.user;

/** The signed in user, as synced from Zitadel. {@code image} is the profile picture, null without one. */
public record UserProfileDto(String id, String name, String email, String image) {
}

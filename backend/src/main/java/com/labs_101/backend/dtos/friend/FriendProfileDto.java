package com.labs_101.backend.dtos.friend;

import com.labs_101.backend.dtos.profile.ProfileDto;

/** The profile page of a friend: the same stats and achievements the friend sees, plus name and training. */
public record FriendProfileDto(FriendDto friend, ProfileDto profile) {
}

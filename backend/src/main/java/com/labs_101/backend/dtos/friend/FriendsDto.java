package com.labs_101.backend.dtos.friend;

import java.util.List;

/**
 * @param friends  the ones training first, then by name
 * @param incoming requests to the user, newest first
 * @param outgoing requests of the user, newest first
 */
public record FriendsDto(List<FriendDto> friends, List<FriendRequestDto> incoming, List<FriendRequestDto> outgoing) {
}

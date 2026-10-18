package com.labs_101.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

import com.labs_101.backend.dtos.friend.FriendProfileDto;
import com.labs_101.backend.dtos.friend.FriendsDto;
import com.labs_101.backend.dtos.friend.SendFriendRequestDto;
import com.labs_101.backend.security.CurrentUser;
import com.labs_101.backend.services.FriendService;

@RestController()
@RequestMapping("/api/users/me/friends")
public class FriendController {

    private final FriendService friendService;

    FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** Friends (with what they are training right now) and the open requests. */
    @GetMapping("")
    public FriendsDto getFriends(@CurrentUser String userId) {
        return friendService.getFriends(userId);
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> sendRequest(@CurrentUser String userId, @RequestBody SendFriendRequestDto dto) {
        friendService.sendRequest(userId, dto.userId());
        return ResponseEntity.noContent().build();
    }

    /** Accepts a request to the user. */
    @PutMapping("/requests/{id}")
    public ResponseEntity<Void> acceptRequest(@CurrentUser String userId, @PathVariable Long id) {
        friendService.acceptRequest(userId, id);
        return ResponseEntity.noContent().build();
    }

    /** Declines a request to the user or takes back one of the user. */
    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@CurrentUser String userId, @PathVariable Long id) {
        friendService.deleteRequest(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@CurrentUser String userId, @PathVariable String friendId) {
        friendService.removeFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }

    /** Streak, heatmap and achievements of a friend, {@code zone} as for the own profile. */
    @GetMapping("/{friendId}/profile")
    public FriendProfileDto getProfile(@CurrentUser String userId, @PathVariable String friendId,
            @RequestParam(required = false) String zone) {
        ZoneId zoneId = UserProfileController.zone(zone);
        return friendService.getProfile(userId, friendId, LocalDate.now(zoneId), zoneId);
    }

    /** Cheers on the running workout of a friend, once per workout. */
    @PostMapping("/{friendId}/cheers")
    public ResponseEntity<Void> cheer(@CurrentUser String userId, @PathVariable String friendId) {
        friendService.cheer(userId, friendId);
        return ResponseEntity.noContent().build();
    }
}

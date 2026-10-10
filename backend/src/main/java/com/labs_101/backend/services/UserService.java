package com.labs_101.backend.services;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.dtos.user.UserProfileDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.ZitadelClient;

@Service
public class UserService {

    static final int SEARCH_LIMIT = 10;

    private final UserRepository userRepository;
    private final ZitadelClient zitadelClient;

    UserService(UserRepository userRepository, ZitadelClient zitadelClient) {
        this.userRepository = userRepository;
        this.zitadelClient = zitadelClient;
    }

    /** At most {@value #SEARCH_LIMIT} users, nothing for an empty query so not everyone can be listed. */
    public List<CalendarUserDto> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.search(query.strip(), PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map((user) -> CalendarMapper.fromUser(user, zitadelClient::avatarUrl)).toList();
    }

    /**
     * Name and email from zitadel with the token of the user, so changes show up right away.
     * Without an answer (or without a token) the stored ones.
     */
    public UserProfileDto getProfile(String userId, String accessToken) {
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.user(userId));
        Map<String, Object> info = accessToken != null ? zitadelClient.userInfo(accessToken) : Map.of();
        return new UserProfileDto(userId,
                string(info, "name", user.getName()),
                string(info, "email", user.getEmail()),
                zitadelClient.avatarUrl(userId));
    }

    private static String string(Map<String, Object> info, String key, String fallback) {
        return info.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }
}

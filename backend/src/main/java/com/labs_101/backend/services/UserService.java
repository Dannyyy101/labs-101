package com.labs_101.backend.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.UserRepository;

@Service
public class UserService {

    static final int SEARCH_LIMIT = 10;

    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** At most {@value #SEARCH_LIMIT} users, nothing for an empty query so not everyone can be listed. */
    public List<CalendarUserDto> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.search(query.strip(), PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(CalendarMapper::fromUser).toList();
    }
}

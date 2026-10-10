package com.labs_101.backend.services;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.settings.SaveUserSettingsDto;
import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.entities.UserSettings;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.UserSettingsMapper;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.UserSettingsRepository;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    UserSettingsService(UserSettingsRepository userSettingsRepository, UserRepository userRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.userRepository = userRepository;
    }

    public UserSettingsDto getSettings(String userId) {
        return UserSettingsMapper.fromUserSettings(findSettings(userId));
    }

    public UserSettingsDto createSettings(String userId, SaveUserSettingsDto dto) {
        if (userSettingsRepository.existsById(userId)) {
            throw BadRequestException.userSettingsExist(userId);
        }
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }
        validate(dto);
        UserSettings settings = new UserSettings(userRepository.getReferenceById(userId), dto.getCalorieGoal());
        return UserSettingsMapper.fromUserSettings(userSettingsRepository.save(settings));
    }

    public UserSettingsDto updateSettings(String userId, SaveUserSettingsDto dto) {
        UserSettings settings = findSettings(userId);
        validate(dto);

        if (dto.getCalorieGoal() != null) {
            settings.setCalorieGoal(dto.getCalorieGoal());
        }

        return UserSettingsMapper.fromUserSettings(userSettingsRepository.save(settings));
    }

    public void deleteSettings(String userId) {
        userSettingsRepository.delete(findSettings(userId));
    }

    private UserSettings findSettings(String userId) {
        return userSettingsRepository.findById(userId).orElseThrow(() -> NotFoundException.userSettings(userId));
    }

    private void validate(SaveUserSettingsDto dto) {
        if (dto.getCalorieGoal() != null && dto.getCalorieGoal() <= 0) {
            throw BadRequestException.calorieGoal(dto.getCalorieGoal());
        }
    }
}

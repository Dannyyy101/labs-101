package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.settings.UserSettingsDto;
import com.labs_101.backend.entities.UserSettings;

public class UserSettingsMapper {
    public static UserSettingsDto fromUserSettings(UserSettings settings) {
        return new UserSettingsDto(settings.getUserId(), settings.getCalorieGoal());
    }
}

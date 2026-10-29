package com.labs_101.backend.dtos.settings;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SaveUserSettingsDto {
    private @Nullable Integer calorieGoal;
}

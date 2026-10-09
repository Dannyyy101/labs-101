package com.labs_101.backend.dtos.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingsDto {
    private String userId;
    private Integer calorieGoal;
}

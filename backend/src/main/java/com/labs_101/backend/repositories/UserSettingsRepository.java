package com.labs_101.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {
}

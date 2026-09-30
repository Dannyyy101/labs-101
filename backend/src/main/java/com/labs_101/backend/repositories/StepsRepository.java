package com.labs_101.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.Steps;
import java.util.Optional;

public interface StepsRepository extends JpaRepository<Steps, Long> {
    Optional<Steps> getByUuid(String uuid);
}

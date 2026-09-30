package com.labs_101.backend.services;

import com.labs_101.backend.repositories.StepsRepository;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.healthData.StepsDto;
import com.labs_101.backend.dtos.healthData.SyncHealthData;
import com.labs_101.backend.entities.Steps;

@Service
public class HealthDataService {
    private final StepsRepository stepsRepository;

    HealthDataService(StepsRepository stepsRepository) {
        this.stepsRepository = stepsRepository;
    }

    public void syncAppleHealth(SyncHealthData dto) {
        dto.items().stream().forEach((healthData) -> {
            switch (healthData.type()) {
                case "STEPS":
                    StepsDto stepsDto = (StepsDto) healthData;
                    Optional<Steps> steps = stepsRepository.getByUuid(stepsDto.uuid());
                    if (steps.isEmpty()) {
                        stepsRepository.save(new Steps(null, stepsDto.uuid(), stepsDto.value(), stepsDto.createdAt(),
                                stepsDto.updatedAt()));
                    } else {
                        Steps entity = steps.get();
                        entity.setValue(stepsDto.value());
                        stepsRepository.save(entity);
                    }
                    break;

                default:
                    break;
            }
        });
    }
}

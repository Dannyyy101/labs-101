package com.labs_101.backend.services;

import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.WorkoutTemplateRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;

@Service
public class WorkoutService {
    private final WorkoutTemplateRepository workoutTemplateRepository;

    WorkoutService(WorkoutTemplateRepository workoutTemplateRepository) {
        this.workoutTemplateRepository = workoutTemplateRepository;
    }

    public void createNewWorkoutTemplate(CreateWorkoutTemplateDto templateDto) {
        workoutTemplateRepository.save(WorkoutMapper.fromCreateRunDto(templateDto));
    }

    public List<GetWorkoutTemplateDto> getAllWorkoutTemplates() {
        return workoutTemplateRepository.findAll().stream()
                .map((template) -> WorkoutMapper.toGetWorkoutTemplateDto(template)).toList();
    }
}

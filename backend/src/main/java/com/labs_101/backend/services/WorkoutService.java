package com.labs_101.backend.services;

import com.labs_101.backend.mapper.ExerciseMapper;
import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.repositories.WorkoutTemplateRepository;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.Workout;

@Service
public class WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;

    WorkoutService(WorkoutTemplateRepository workoutTemplateRepository, ExerciseRepository exerciseRepository,
            WorkoutRepository workoutRepository) {
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
    }

    public void createNewWorkoutTemplate(CreateWorkoutTemplateDto templateDto) {
        workoutTemplateRepository.save(WorkoutMapper.fromCreateRunDto(templateDto));
    }

    public List<GetWorkoutTemplateDto> getAllWorkoutTemplates() {
        return workoutTemplateRepository.findAll().stream()
                .map((template) -> WorkoutMapper.toGetWorkoutTemplateDto(template)).toList();
    }

    public void createExercise(CreateExerciseDto exerciseDto) {
        exerciseRepository.save(ExerciseMapper.fromCreateExerciseDto(exerciseDto));
    }

    public List<ExerciseDto> getAllExercises() {
        return exerciseRepository.findAll().stream().map((exercise) -> ExerciseMapper.fromExercise(exercise)).toList();
    }

    public WorkoutDto createWorkout(CreateWorkoutDto workoutDto) {
        List<Exercise> exercises = exerciseRepository.findAllById(
                workoutDto.getItems().stream()
                        .map(item -> item.exerciseId())
                        .toList());
        Workout workout = workoutRepository.save(WorkoutMapper.fromCreateWorkoutDto(workoutDto, exercises));
        return WorkoutMapper.toWorkoutDto(workout);
    }
}

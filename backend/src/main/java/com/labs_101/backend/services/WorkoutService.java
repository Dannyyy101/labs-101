package com.labs_101.backend.services;

import com.labs_101.backend.mapper.BodyPartMapper;
import com.labs_101.backend.mapper.ExerciseMapper;
import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.repositories.WorkoutTemplateRepository;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.BodyPartDto;
import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.entities.BodyPart;
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

    @Transactional
    public void createExercise(CreateExerciseDto exerciseDto) {
        Exercise exercise = ExerciseMapper.fromCreateExerciseDto(exerciseDto);
        exercise.getBodyParts().forEach(part -> part.setExercise(exercise));
        exerciseRepository.save(exercise);
    }

    public List<ExerciseDto> getAllExercises(String name, String type) {
        List<Specification<Exercise>> specs = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (type != null && !type.isBlank()) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("type")), type.toLowerCase()));
        }

        Specification<Exercise> spec = Specification.allOf(specs);

        return exerciseRepository.findAll(spec).stream()
                .map(ExerciseMapper::fromExercise)
                .toList();
    }

    public WorkoutDto createWorkout(CreateWorkoutDto workoutDto) {
        List<Exercise> exercises = exerciseRepository.findAllById(
                workoutDto.getItems().stream()
                        .map(item -> item.exerciseId())
                        .toList());
        Workout workout = workoutRepository.save(WorkoutMapper.fromCreateWorkoutDto(workoutDto, exercises));
        return WorkoutMapper.toWorkoutDto(workout);
    }

    public void deleteExercise(Long id) {
        exerciseRepository.deleteById(id);
    }

    public ExerciseDto updateExerciseById(ExerciseDto exerciseDto) {
        Exercise model = exerciseRepository.findById(exerciseDto.getId()).orElseThrow();
        if (!exerciseDto.getName().equals(model.getName())) {
            model.setName(exerciseDto.getName());
        }
        if (!exerciseDto.getDescription().equals(model.getDescription())) {
            model.setDescription(exerciseDto.getDescription());
        }

        syncBodyParts(model, exerciseDto.getBodyParts());

        return ExerciseMapper.fromExercise(exerciseRepository.save(model));
    }

    private void syncBodyParts(Exercise exercise, List<BodyPartDto> incoming) {
        Map<Long, BodyPartDto> byId = incoming.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(BodyPartDto::getId, d -> d));

        exercise.getBodyParts().removeIf(part -> !byId.containsKey(part.getId()));

        exercise.getBodyParts().forEach(part -> {
            BodyPartDto d = byId.get(part.getId());
            part.setColor(d.getColor());
            part.setIntensity(d.getIntensity());
        });

        incoming.stream()
                .filter(d -> d.getId() == null)
                .forEach(d -> {
                    BodyPart part = BodyPartMapper.fromDto(d);
                    part.setExercise(exercise);
                    exercise.getBodyParts().add(part);
                });
    }
}

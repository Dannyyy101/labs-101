package com.labs_101.backend.services;

import com.labs_101.backend.mapper.BodyPartMapper;
import com.labs_101.backend.mapper.ExerciseMapper;
import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.WorkoutRepository;

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
import com.labs_101.backend.dtos.workout.ExerciseItem;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.entities.workout.workoutExercises.StrengthItem;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.dtos.workout.WorkoutHeaderDto;
import com.labs_101.backend.entities.BodyPart;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutExercise;

@Service
public class WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;

    WorkoutService(ExerciseRepository exerciseRepository,
            WorkoutRepository workoutRepository) {
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
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

    @Transactional
    public Workout create(CreateWorkoutDto request) {
        Workout workout = new Workout();
        workout.setName(request.name());
        workout.setDuration(0.0);

        for (ExerciseItem item : request.exercises()) {
            Exercise exercise = exerciseRepository.getReferenceById(item.exercise().getId());
            workout.addExercise(toEntity(exercise, item));
        }

        return workoutRepository.save(workout);
    }

    public List<WorkoutHeaderDto> getAll() {
        List<Workout> workouts = workoutRepository.findAll();

        return workouts.stream().map((workout) -> WorkoutMapper.fromWorkoutToWorkoutHeaderDto(workout)).toList();
    }

    public WorkoutDto getById(Long id) {
        Workout workout = workoutRepository.findById(id).orElseThrow(() -> NotFoundException.workout(id));
        return WorkoutMapper.fromWorkoutToWorkoutDto(workout);
    }

    private WorkoutExercise<?> toEntity(Exercise exercise, ExerciseItem item) {
        return switch (item) {
            case StrengthItemDto s -> new StrengthItem(
                    exercise,
                    new StrengthItem.Settings(
                            s.sets().stream()
                                    .map(d -> new StrengthItem.Set(d.order(), d.reps(), d.weightKg(), d.rpe()))
                                    .toList()));
            default ->
                null;

        };
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

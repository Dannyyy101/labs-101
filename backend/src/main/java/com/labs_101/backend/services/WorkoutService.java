package com.labs_101.backend.services;

import com.labs_101.backend.mapper.BodyPartMapper;
import com.labs_101.backend.mapper.ExerciseMapper;
import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.CalendarRepository;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.repositories.WorkoutSessionRepository;

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
import com.labs_101.backend.exception.ForbiddenException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.entities.BodyPart;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutExercise;

@Service
public class WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;

    WorkoutService(ExerciseRepository exerciseRepository,
            WorkoutRepository workoutRepository, WorkoutSessionRepository sessionRepository,
            CalendarRepository calendarRepository, UserRepository userRepository) {
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.calendarRepository = calendarRepository;
        this.userRepository = userRepository;
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
    public WorkoutDto create(String userId, CreateWorkoutDto request) {
        Workout workout = new Workout();
        workout.setDuration(0.0);
        workout.setOwner(userRepository.getReferenceById(userId));
        apply(workout, request);
        return WorkoutMapper.fromWorkoutToWorkoutDto(workoutRepository.save(workout), userId);
    }

    /** Replaces name and exercises, sessions started from the workout keep their copy. */
    @Transactional
    public WorkoutDto update(String userId, Long id, CreateWorkoutDto request) {
        Workout workout = findEditable(userId, id);
        workout.getExercises().clear();
        // removes the old rows before the new ones are inserted
        workoutRepository.flush();
        apply(workout, request);
        return WorkoutMapper.fromWorkoutToWorkoutDto(workoutRepository.save(workout), userId);
    }

    @Transactional
    public void delete(String userId, Long id) {
        Workout workout = findEditable(userId, id);
        // the history stays, only the link to the template is removed
        sessionRepository.findByWorkout_Id(id).forEach((session) -> session.setWorkout(null));
        // planned events stay in the calendar without a workout
        calendarRepository.findByWorkout_Id(id).forEach((event) -> event.setWorkout(null));
        workoutRepository.delete(workout);
    }

    @Transactional
    public List<WorkoutDto> getAll(String userId) {
        return workoutRepository.findAll().stream().map((workout) -> WorkoutMapper.fromWorkoutToWorkoutDto(workout, userId)).toList();
    }

    @Transactional
    public WorkoutDto getById(String userId, Long id) {
        Workout workout = workoutRepository.findById(id).orElseThrow(() -> NotFoundException.workout(id));
        return WorkoutMapper.fromWorkoutToWorkoutDto(workout, userId);
    }

    private Workout findEditable(String userId, Long id) {
        Workout workout = workoutRepository.findById(id).orElseThrow(() -> NotFoundException.workout(id));
        if (!workout.isEditableBy(userId)) {
            throw ForbiddenException.workout(id);
        }
        return workout;
    }

    private void apply(Workout workout, CreateWorkoutDto request) {
        workout.setName(request.name() == null || request.name().isBlank() ? "Workout" : request.name().trim());
        for (ExerciseItem item : request.exercises()) {
            Exercise exercise = exerciseRepository.findById(item.exercise().getId())
                    .orElseThrow(() -> NotFoundException.exercise(item.exercise().getId()));
            workout.addExercise(toEntity(exercise, item));
        }
    }

    private WorkoutExercise<?> toEntity(Exercise exercise, ExerciseItem item) {
        return switch (item) {
            case StrengthItemDto s -> new StrengthItem(
                    exercise,
                    new StrengthItem.Settings(
                            s.sets().stream()
                                    .map(d -> new StrengthItem.Set(d.order(), d.reps(), d.weightKg(), d.rpe(), d.restSeconds()))
                                    .toList()));
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

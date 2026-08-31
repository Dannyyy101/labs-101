package com.labs_101.backend.controller;

import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.services.WorkoutService;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/workouts")
public class WorkoutController {
    private final WorkoutService workoutService;

    WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping("")
    public WorkoutDto createWorkout(@RequestBody CreateWorkoutDto workoutDto) {
        try {
            return workoutService.createWorkout(workoutDto);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/exercises")
    public ResponseEntity<Void> createExercise(@RequestBody CreateExerciseDto exerciseDto) {
        try {
            workoutService.createExercise(exerciseDto);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/exercises")
    public List<ExerciseDto> getAllExercises(@RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        try {
            return workoutService.getAllExercises(name, type);
        } catch (Exception e) {
            throw e;
        }
    }

    @PutMapping("/exercises/{id}")
    public ExerciseDto updateExerciseById(@PathVariable String id, @RequestBody ExerciseDto exerciseDto) {
        Long exerciseId = Long.parseLong(id);
        if (!exerciseId.equals(exerciseDto.getId())) {
            throw new Error("Ids must be equal");
        }

        try {
            return workoutService.updateExerciseById(exerciseDto);
        } catch (Exception e) {
            throw e;
        }
    }

    @DeleteMapping("/exercises/{id}")
    public ResponseEntity<Void> deleteExercise(@PathVariable String id) {
        try {
            workoutService.deleteExercise(Long.parseLong(id));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/templates")
    public ResponseEntity<Void> createWorkoutTemplate(@RequestBody CreateWorkoutTemplateDto templateDto) {
        try {
            workoutService.createNewWorkoutTemplate(templateDto);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/templates")
    public List<GetWorkoutTemplateDto> getAllWorkoutTemplates() {
        try {
            return workoutService.getAllWorkoutTemplates();
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<Void> createWorkoutSession(@RequestBody CreateWorkoutSessionDto workoutSessionDto) {
        try {
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw e;
        }
    }
}

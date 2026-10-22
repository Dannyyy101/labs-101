package com.labs_101.backend.controller;

import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.security.CurrentUser;
import com.labs_101.backend.services.WorkoutService;

import java.util.List;

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
    public WorkoutDto createWorkout(@CurrentUser String userId, @RequestBody CreateWorkoutDto workoutDto) {
        return workoutService.create(userId, workoutDto);
    }

    @GetMapping("{id}")
    public WorkoutDto getWorkoutById(@CurrentUser String userId, @PathVariable Long id) {
        return workoutService.getById(userId, id);
    }

    /** Only the creator of the template can change it. */
    @PutMapping("{id}")
    public WorkoutDto updateWorkout(@CurrentUser String userId, @PathVariable Long id, @RequestBody CreateWorkoutDto workoutDto) {
        return workoutService.update(userId, id, workoutDto);
    }

    /** Only the creator of the template can delete it. */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteWorkout(@CurrentUser String userId, @PathVariable Long id) {
        workoutService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("")
    public List<WorkoutDto> getAllWorkouts(@CurrentUser String userId) {
        return workoutService.getAll(userId);
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
}

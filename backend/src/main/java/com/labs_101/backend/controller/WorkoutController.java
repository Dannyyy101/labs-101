package com.labs_101.backend.controller;

import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.entities.workoutTemplate.WorkoutTemplate;
import com.labs_101.backend.services.WorkoutService;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/workouts")
public class WorkoutController {
    private final WorkoutService workoutService;

    WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> createRun(@RequestBody ArrayList<Object> run) {
        try {
            System.out.println(run);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
}

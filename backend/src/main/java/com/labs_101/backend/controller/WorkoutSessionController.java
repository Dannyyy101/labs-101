package com.labs_101.backend.controller;

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

import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.UpdateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.services.WorkoutSessionService;
import com.labs_101.backend.security.CurrentUser;

/** Workouts the user did or is doing right now, see {@link WorkoutController} for the templates. */
@RestController()
@RequestMapping("/api/users/me/workout-sessions")
public class WorkoutSessionController {

    private final WorkoutSessionService sessionService;

    WorkoutSessionController(WorkoutSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Finished workouts started in the range, newest first. */
    @GetMapping
    public List<WorkoutSessionDto> getSessions(@CurrentUser String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return sessionService.getSessions(userId, from, to);
    }

    /** The running workout, 204 when there is none. */
    @GetMapping("/active")
    public ResponseEntity<WorkoutSessionDto> getActive(@CurrentUser String userId) {
        return sessionService.getActive(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public WorkoutSessionDto getSession(@CurrentUser String userId, @PathVariable Long id) {
        return sessionService.getSession(userId, id);
    }

    @PostMapping
    public WorkoutSessionDto start(@CurrentUser String userId, @RequestBody CreateWorkoutSessionDto request) {
        return sessionService.start(userId, request);
    }

    @PutMapping("/{id}")
    public WorkoutSessionDto update(@CurrentUser String userId, @PathVariable Long id,
            @RequestBody UpdateWorkoutSessionDto request) {
        return sessionService.update(userId, id, request);
    }

    @PostMapping("/{id}/finish")
    public WorkoutSessionDto finish(@CurrentUser String userId, @PathVariable Long id,
            @RequestBody(required = false) UpdateWorkoutSessionDto request) {
        return sessionService.finish(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser String userId, @PathVariable Long id) {
        sessionService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}

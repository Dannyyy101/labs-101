package com.labs_101.backend.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.run.RunDetailDto;
import com.labs_101.backend.dtos.run.RunSummaryDto;
import com.labs_101.backend.services.RunService;

/** Running workouts out of the synced Apple Health data, see {@link HealthController}. */
@RestController()
@RequestMapping("/api/users/{userId}/runs")
public class RunController {

    private final RunService runService;

    RunController(RunService runService) {
        this.runService = runService;
    }

    /** Runs overlapping the range, newest first. */
    @GetMapping
    public List<RunSummaryDto> getRuns(@PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return runService.getRuns(userId, from, to);
    }

    /** The run with its resampled track, splits and heart rate. */
    @GetMapping("/{id}")
    public RunDetailDto getRun(@PathVariable String userId, @PathVariable UUID id) {
        return runService.getRun(userId, id);
    }
}

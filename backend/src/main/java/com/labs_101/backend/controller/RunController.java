package com.labs_101.backend.controller;

import java.util.ArrayList;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.entities.Run;
import com.labs_101.backend.services.RunService;

@RestController()
@RequestMapping("/api/runs")
public class RunController {

  private final RunService runService;

  public RunController(RunService runService) {
    this.runService = runService;
  }

  @GetMapping("")
  public ArrayList<Run> getAllRuns() {
    return runService.getAllRuns();
  }

  @GetMapping("")
  public ArrayList<Run> createRun() {
    return runService.getAllRuns();
  }

}
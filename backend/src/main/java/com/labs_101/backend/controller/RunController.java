package com.labs_101.backend.controller;

import java.util.ArrayList;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.CreateRunDto;
import com.labs_101.backend.entities.Run;
import com.labs_101.backend.services.RunService;
import org.springframework.web.bind.annotation.PostMapping;


@RestController()
@RequestMapping("/api/runs")
public class RunController {

  private final RunService runService;

  public RunController(RunService runService) {
    this.runService = runService;
  }

  @GetMapping("")
  public ArrayList<Run> getAllRuns() {
    ArrayList<Run> temp = runService.getAllRuns();
    System.out.println(temp.get(0));
    return temp;
  }

  @PostMapping("")
  public ResponseEntity<Void> createRun(@RequestBody CreateRunDto run) {
    try {
      runService.createRun(run);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

}
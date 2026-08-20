package com.labs_101.backend.services;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.CreateRunDto;
import com.labs_101.backend.entities.Run;
import com.labs_101.backend.mapper.RunMapper;
import com.labs_101.backend.repositories.RunRepository;

@Service
public class RunService {
    private final RunRepository runRepository;

    public RunService(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public ArrayList<Run> getAllRuns(){
        return (ArrayList<Run>) runRepository.findAll();
    }

    public void createRun(CreateRunDto run){
        runRepository.save(RunMapper.fromCreateRunDto(run));
    }
}

package com.labs_101.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.labs_101.backend.entities.workoutTemplate.WorkoutTemplate;

public interface WorkoutTemplateRepository extends MongoRepository<WorkoutTemplate, String>{
    
}

package com.labs_101.backend.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.labs_101.backend.entities.Exercise;

public interface ExerciseRepository extends MongoRepository<Exercise, String> {
}

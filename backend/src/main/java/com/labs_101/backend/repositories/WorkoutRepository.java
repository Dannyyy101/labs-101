package com.labs_101.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.labs_101.backend.entities.workout.Workout;

public interface WorkoutRepository extends MongoRepository<Workout, String> {

}
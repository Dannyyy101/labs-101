package com.labs_101.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.workout.Workout;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

}
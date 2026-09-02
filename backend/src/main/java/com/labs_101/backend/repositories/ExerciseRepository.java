package com.labs_101.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.Exercise;

public interface ExerciseRepository
        extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {
}
package com.labs_101.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.labs_101.backend.entities.Run;

public interface RunRepository extends MongoRepository<Run, String> {

	Page<Run> findAll(Pageable pageable);
}
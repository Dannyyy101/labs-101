package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.FoodUser;

public interface FoodUserRepository extends JpaRepository<FoodUser, Long> {
    List<FoodUser> findByUser_IdAndCreateDateBetween(
            String userId, Instant start, Instant end);
}

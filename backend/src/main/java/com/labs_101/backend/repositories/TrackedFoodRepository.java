package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.food.TrackedFood;

public interface TrackedFoodRepository extends JpaRepository<TrackedFood, Long> {
    List<TrackedFood> findByUser_IdAndCreateDateBetween(
            String userId, Instant start, Instant end);

    Optional<TrackedFood> findFirstByUser_IdAndFood_IdOrderByCreateDateDesc(String userId, Long foodId);
}

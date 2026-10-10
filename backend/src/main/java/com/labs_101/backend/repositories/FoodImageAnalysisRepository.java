package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.food.FoodImageAnalysis;
import com.labs_101.backend.entities.food.FoodImageStatus;

public interface FoodImageAnalysisRepository extends JpaRepository<FoodImageAnalysis, Long> {

    Optional<FoodImageAnalysis> findByIdAndUserId(Long id, String userId);

    List<FoodImageAnalysis> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndCreatedAtGreaterThanEqualAndStatusNotIn(String userId, Instant from,
            Collection<FoodImageStatus> statuses);

    /** e.g. {@code findByStatusOrderByNextAttemptAt(PENDING)} to continue after a restart */
    List<Pending> findByStatusOrderByNextAttemptAt(FoodImageStatus status);

    record Pending(Long id, Instant nextAttemptAt) {
    }
}

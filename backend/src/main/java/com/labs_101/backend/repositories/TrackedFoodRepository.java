package com.labs_101.backend.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.food.TrackedFood;

public interface TrackedFoodRepository extends JpaRepository<TrackedFood, Long> {
    List<TrackedFood> findByUser_IdAndCreateDateBetween(
            String userId, Instant start, Instant end);

    Optional<TrackedFood> findFirstByUser_IdAndFood_IdOrderByCreateDateDesc(String userId, Long foodId);

    /** Moves an entry to another time, saving the entity never updates its creation timestamp. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TrackedFood t SET t.createDate = :createDate WHERE t.id = :id")
    int moveTo(@Param("id") Long id, @Param("createDate") Instant createDate);
}

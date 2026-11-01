package com.labs_101.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.workout.Workout;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    /** The templates the user sees, see {@link Workout#isVisibleTo}. */
    @Query("""
            select distinct w from Workout w left join w.sharedWith s
            where w.owner is null or w.owner.id = :userId or s.id = :userId
            order by w.id
            """)
    List<Workout> findVisibleTo(@Param("userId") String userId);
}

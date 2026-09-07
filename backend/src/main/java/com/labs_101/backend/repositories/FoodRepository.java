package com.labs_101.backend.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import com.labs_101.backend.entities.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
    @Query("""
            SELECT f FROM Food f
            LEFT JOIN f.foodUsers fu WITH fu.user.id = :userId
            WHERE f.name LIKE CONCAT('%', :name, '%')
            GROUP BY f
            ORDER BY COUNT(fu) DESC
            """)
    List<Food> findAllByNameAndUserId(@Param("name") String name, @Param("userId") String userId);
}
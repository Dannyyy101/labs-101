package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.food.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
        @Query(value = """
                        SELECT f.* FROM food f
                        LEFT JOIN food_user fu ON fu.food_id = f.id AND fu.user_id = :userId
                        WHERE lower(f.name) % :name
                        GROUP BY f.id
                        ORDER BY similarity(lower(f.name), :name) DESC, COUNT(fu.id) DESC, f.name ASC
                        """, countQuery = """
                        SELECT count(*) FROM food f
                        WHERE lower(f.name) % :name
                        """, nativeQuery = true)
        Page<Food> findAllByNameAndUserId(Pageable p, @Param("name") String name, @Param("userId") String userId);

        Page<Food> findByNameContainingIgnoreCase(String name, Pageable p);

        Optional<Food> findByBarCode(String barcode);

        @Query("""
                                    SELECT f FROM TrackedFood fu
                        JOIN fu.food f
                        WHERE fu.user.id = :userId
                        GROUP BY f, fu.createDate
                        ORDER BY fu.createDate DESC
                                    """)
        Page<Food> findAllByLastUsed(Pageable p, String userId);

        @Query(value = """
                        SELECT * FROM food
                        WHERE lower(name) % :query
                        ORDER BY similarity(lower(name), :query) DESC
                        LIMIT 1
                        """, nativeQuery = true)
        Food findSimilar(@Param("query") String query);
}
package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.food.OpenFood;

public interface OpenFoodRepository extends JpaRepository<OpenFood, Long> {
    Optional<OpenFood> findByBarCode(String barcode);

    /**
     * Only entries with calories are considered, on equal score the one with
     * the most complete nutrition wins.
     *
     * @param query lowercase food name
     */
    @Query(value = "SELECT id, name, " + FoodMatchProjection.SCORE + " AS score FROM open_food "
            + "WHERE " + FoodMatchProjection.CANDIDATES + " AND kcal IS NOT NULL"
            + " ORDER BY score DESC,"
            + " num_nonnulls(protein, fat, carbohydrates) DESC, id"
            + " LIMIT 1", nativeQuery = true)
    Optional<FoodMatchProjection> findBestMatch(@Param("query") String query);
}

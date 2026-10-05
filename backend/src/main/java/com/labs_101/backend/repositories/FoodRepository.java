package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.food.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
        /**
         * Our foods and the open food database ranked together by
         * {@link FoodMatchProjection#SCORE}, like the food extractor does. Our
         * foods also match by substring, so short inputs like "a" or parts of a
         * word like "pfe" are found too. They get a bonus, foods the user has
         * tracked before an even bigger one. Open foods that were already
         * imported only show up as food.
         */
        String SEARCH = "SELECT f.id, f.name, false AS open_food, " + FoodMatchProjection.SCORE
                        + " + " + FoodMatchProjection.FOOD_BONUS
                        + " + CASE WHEN EXISTS (SELECT 1 FROM food_user fu"
                        + "     WHERE fu.food_id = f.id AND fu.user_id = :userId) THEN 0.3 ELSE 0 END AS score"
                        + " FROM food f"
                        + " WHERE " + FoodMatchProjection.CANDIDATES + " OR strpos(lower(f.name), :query) > 0"
                        + " UNION ALL"
                        + " SELECT o.id, o.name, true, " + FoodMatchProjection.SCORE
                        + " FROM open_food o"
                        + " WHERE " + FoodMatchProjection.CANDIDATES + " AND o.kcal IS NOT NULL"
                        + " AND NOT EXISTS (SELECT 1 FROM food f WHERE f.bar_code = o.bar_code)";

        String SEARCH_FILTER = " WHERE NOT m.open_food OR m.score >= " + FoodMatchProjection.MIN_SCORE;

        /**
         * @param query lowercase food name
         */
        @Query(value = "SELECT m.id, m.name, m.open_food AS \"openFood\" FROM (" + SEARCH + ") m" + SEARCH_FILTER
                        + " ORDER BY m.score DESC, m.open_food, m.name, m.id",
                        countQuery = "SELECT count(*) FROM (" + SEARCH + ") m" + SEARCH_FILTER,
                        nativeQuery = true)
        Page<SearchFoodProjection> search(Pageable p, @Param("query") String query, @Param("userId") String userId);

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

        /**
         * @param query lowercase food name
         */
        @Query(value = "SELECT id, name, " + FoodMatchProjection.SCORE + " AS score FROM food "
                        + "WHERE " + FoodMatchProjection.CANDIDATES
                        + " ORDER BY score DESC, id LIMIT 1", nativeQuery = true)
        Optional<FoodMatchProjection> findBestMatch(@Param("query") String query);
}
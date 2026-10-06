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
         * Our foods and the open food database ranked by
         * {@link FoodMatchProjection#SCORE}, like the food extractor does. Our
         * foods always come first, open foods after them. Our
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

        /** names that only differ in case or spaces are the same */
        String SEARCH_NAME = "lower(trim(regexp_replace(m.name, '\\s+', ' ', 'g')))";

        /**
         * Open food has many entries with the same name, so every name only
         * shows up once: the best match, our foods before open foods.
         */
        String UNIQUE_SEARCH = "SELECT DISTINCT ON (" + SEARCH_NAME + ") m.* FROM (" + SEARCH + ") m" + SEARCH_FILTER
                        + " ORDER BY " + SEARCH_NAME + ", m.score DESC, m.open_food, m.id";

        /**
         * @param query lowercase food name
         */
        @Query(value = "SELECT u.id, u.name, u.open_food AS \"openFood\" FROM (" + UNIQUE_SEARCH + ") u"
                        + " ORDER BY u.open_food, u.score DESC, u.name, u.id",
                        countQuery = "SELECT count(DISTINCT " + SEARCH_NAME + ") FROM (" + SEARCH + ") m" + SEARCH_FILTER,
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
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
         * Fuzzy (trigram) or substring match, so short inputs like "a" or parts
         * of a word like "pfe" are found too. Foods the user has tracked before
         * and names starting with the input come first.
         */
        @Query(value = """
                        SELECT f.* FROM food f
                        LEFT JOIN food_user fu ON fu.food_id = f.id AND fu.user_id = :userId
                        WHERE lower(f.name) % lower(:name) OR strpos(lower(f.name), lower(:name)) > 0
                        GROUP BY f.id
                        ORDER BY similarity(lower(f.name), lower(:name))
                                + CASE WHEN starts_with(lower(f.name), lower(:name)) THEN 0.2 ELSE 0 END
                                + CASE WHEN COUNT(fu.id) > 0 THEN 0.3 ELSE 0 END DESC,
                                COUNT(fu.id) DESC, f.name ASC
                        """, countQuery = """
                        SELECT count(*) FROM food f
                        WHERE lower(f.name) % lower(:name) OR strpos(lower(f.name), lower(:name)) > 0
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

        /**
         * @param query lowercase food name
         */
        @Query(value = "SELECT id, name, " + FoodMatchProjection.SCORE + " AS score FROM food "
                        + "WHERE " + FoodMatchProjection.CANDIDATES
                        + " ORDER BY score DESC, id LIMIT 1", nativeQuery = true)
        Optional<FoodMatchProjection> findBestMatch(@Param("query") String query);
}
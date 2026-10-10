package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.food.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
        /**
         * How many of our foods the score is computed for, the best ones by
         * {@link FoodMatchProjection#SIMILARITY}.
         */
        int FOOD_CANDIDATES = 200;

        /**
         * Shorter queries only search our foods. Their few trigrams match a
         * large part of the open food database, checking all of them takes
         * seconds and the matches are useless anyway.
         */
        int OPEN_FOOD_MIN_QUERY_LENGTH = 3;

        /**
         * {@code :query} as LIKE pattern for names containing it. LIKE can use the
         * trigram index, strpos can't.
         */
        String CONTAINS_QUERY = "'%' || replace(replace(replace(:query, '\\', '\\\\'), '%', '\\%'), '_', '\\_') || '%'";

        /**
         * Our foods and the open food database ranked by
         * {@link FoodMatchProjection#SCORE}, like the food extractor does. Our
         * foods always come first, open foods after them. Our
         * foods also match by substring, so short inputs like "a" or parts of a
         * word like "pfe" are found too. They get a bonus, foods the user has
         * tracked before an even bigger one. Open foods that were already
         * imported only show up as food.
         * <p>
         * The score is expensive, so for our foods it is only computed for the
         * {@link #FOOD_CANDIDATES} best ones, the user's tracked foods first.
         * Open foods are only searched from
         * {@link #OPEN_FOOD_MIN_QUERY_LENGTH} characters on.
         */
        String SEARCH = "SELECT c.id, c.name, false AS open_food, " + FoodMatchProjection.SCORE
                        + " + " + FoodMatchProjection.FOOD_BONUS
                        + " + CASE WHEN c.tracked THEN 0.3 ELSE 0 END AS score"
                        + " FROM (SELECT f.id, f.name, EXISTS (SELECT 1 FROM food_user fu"
                        + "     WHERE fu.food_id = f.id AND fu.user_id = :userId) AS tracked"
                        + "   FROM food f"
                        + "   WHERE " + FoodMatchProjection.CANDIDATES + " OR lower(f.name) LIKE " + CONTAINS_QUERY
                        + "   ORDER BY tracked DESC, " + FoodMatchProjection.SIMILARITY + " DESC, f.id"
                        + "   LIMIT " + FOOD_CANDIDATES + ") c"
                        + " UNION ALL"
                        + " SELECT o.id, o.name, true, " + FoodMatchProjection.SCORE
                        + " FROM open_food o"
                        + " WHERE length(:query) >= " + OPEN_FOOD_MIN_QUERY_LENGTH
                        + " AND " + FoodMatchProjection.CANDIDATES + " AND o.kcal IS NOT NULL"
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
         * A slice, counting all results would run the whole search a second time.
         *
         * @param query lowercase food name
         */
        @Query(value = "SELECT u.id, u.name, u.open_food AS \"openFood\" FROM (" + UNIQUE_SEARCH + ") u"
                        + " ORDER BY u.open_food, u.score DESC, u.name, u.id",
                        nativeQuery = true)
        Slice<SearchFoodProjection> search(Pageable p, @Param("query") String query, @Param("userId") String userId);

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
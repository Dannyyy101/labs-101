package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.UserRepository;

/**
 * Times {@link FoodRepository#search} on a database about the size of
 * production and prints the query plans.
 * Seeding takes a while, so it only runs with
 * {@code ./mvnw test -Dtest=EmbeddedPostgresFoodSearchPerformanceTest -Dperf=true}.
 * The number of rows can be changed with {@code -Dperf.openFood=...} and
 * {@code -Dperf.food=...}.
 */
@Tag("performance")
@EnabledIfSystemProperty(named = "perf", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
public class EmbeddedPostgresFoodSearchPerformanceTest {
    private static final int OPEN_FOODS = Integer.getInteger("perf.openFood", 300_000);
    private static final int FOODS = Integer.getInteger("perf.food", 8_000);
    private static final int TRACKED_FOODS = 2_000;
    private static final int RUNS = 5;
    /** one search request */
    private static final long MAX_MILLIS = 300;

    private static final String WORDS = "array['banane','apfel','haferflocken','joghurt','hähnchenbrust','nutella',"
            + "'käse','milch','brot','reis','nudeln','tomate','gurke','kartoffel','lachs','ei','butter','quark',"
            + "'müsli','schokolade','erdbeere','orange','paprika','zucchini','linsen','kichererbsen','tofu',"
            + "'mandeln','walnüsse','honig']";
    private static final String KINDS = "array['roh','gekocht','natur','vollkorn','bio','light','mehl','pulver',"
            + "'getrocknet','geräuchert','mit milch','ohne zucker','griechisch','fettarm','tiefgekühlt']";

    private static final List<String> QUERIES = List.of("a", "ba", "pfe", "banane", "haferflocken",
            "hähnchenbrust", "joghurt natur", "nutella", "xyzqwv");

    /** what Spring Data runs for one slice, see {@link FoodRepository#search} */
    private static final String SLICE_SQL = "SELECT u.id, u.name, u.open_food FROM (" + FoodRepository.UNIQUE_SEARCH
            + ") u ORDER BY u.open_food, u.score DESC, u.name, u.id LIMIT 21";

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbc;

    private String userId;

    @BeforeEach
    void seed() {
        userId = userRepository.saveAndFlush(new User("perf-user")).getId();

        long start = System.nanoTime();
        // names like "Haferflocken vollkorn, Marke 17 (500 g)", every combination many times like in open food.
        // Like in the real open food database (4.2 million rows) a word like "banane" is in about 1 % of the
        // names, so 30 % start with one of the 30 words, the others with a random made up word.
        jdbc.update("INSERT INTO open_food (bar_code, name, kcal, create_date, update_date)"
                + " SELECT 'b' || g, initcap(CASE WHEN g % 10 < 3 THEN w[1 + (g / 10) % 30]"
                + "     ELSE translate(substr(md5(g::text), 1, 4 + g % 6), '0123456789', 'ghijklmnop') END)"
                + "   || ' ' || k[1 + (g / 30) % 15]"
                + "   || CASE WHEN g % 30 = 0 THEN ' ' || w[1 + (g / 7) % 30] ELSE '' END"
                + "   || ', Marke ' || (g % 500) || ' (' || (100 + g % 900) || ' g)',"
                + "   CASE WHEN g % 11 = 0 THEN NULL ELSE 100 END, now(), now()"
                + " FROM generate_series(1, ?) g, (SELECT " + WORDS + " w, " + KINDS + " k) a", OPEN_FOODS);
        // BLS style names like "Banane roh" or "Käse (Gouda) natur, 45 % Fett"
        jdbc.update("INSERT INTO food (name, kcal, bar_code, create_date, update_date)"
                + " SELECT initcap(w[1 + g % 30]) || CASE WHEN g % 4 = 0 THEN ' (' || w[1 + (g / 3) % 30] || ')' ELSE '' END"
                + "   || ' ' || k[1 + (g / 30) % 15] || ', ' || (g % 50) || ' % Fett',"
                + "   100, CASE WHEN g % 20 = 0 THEN 'b' || (g * 7) ELSE NULL END, now(), now()"
                + " FROM generate_series(1, ?) g, (SELECT " + WORDS + " w, " + KINDS + " k) a", FOODS);
        jdbc.update("INSERT INTO food_user (user_id, food_id, amount, create_date, update_date)"
                + " SELECT ?, (SELECT min(id) FROM food) + (g * 13) % ?, 100, now(), now() FROM generate_series(1, ?) g",
                userId, FOODS, TRACKED_FOODS);
        jdbc.execute("ANALYZE open_food");
        jdbc.execute("ANALYZE food");
        jdbc.execute("ANALYZE food_user");
        System.out.printf("seeded %d open foods, %d foods in %d ms%n", OPEN_FOODS, FOODS,
                (System.nanoTime() - start) / 1_000_000);
    }

    @Test
    void testSearchIsFast() {
        List<String> slow = new ArrayList<>();
        System.out.printf("%n%-16s %8s %8s %8s%n", "query", "median", "max", "found");
        for (String query : QUERIES) {
            foodRepository.search(PageRequest.of(0, 20), query, userId); // warm up
            long[] millis = new long[RUNS];
            int found = 0;
            for (int i = 0; i < RUNS; i++) {
                long start = System.nanoTime();
                found = foodRepository.search(PageRequest.of(0, 20), query, userId).getNumberOfElements();
                millis[i] = (System.nanoTime() - start) / 1_000_000;
            }
            Arrays.sort(millis);
            long median = millis[RUNS / 2];
            System.out.printf("%-16s %6d ms %6d ms %8d%n", query, median, millis[RUNS - 1], found);
            if (median > MAX_MILLIS) {
                slow.add(query + " (" + median + " ms)");
            }
        }

        assertTrue(slow.isEmpty(), "search slower than " + MAX_MILLIS + " ms for " + slow);
    }

    @Test
    void printQueryPlans() {
        for (String query : List.of("pfe", "banane")) {
            System.out.println("\n===== " + query + " =====\n" + explain(SLICE_SQL, query));
        }
    }

    private String explain(String sql, String query) {
        return String.join("\n", namedJdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS) " + sql,
                Map.of("query", query, "userId", userId), String.class));
    }
}

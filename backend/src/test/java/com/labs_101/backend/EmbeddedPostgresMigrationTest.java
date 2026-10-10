package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
public class EmbeddedPostgresMigrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void testPgTrgmIsEnabled() {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class));
    }

    @Test
    void testTrigramIndexesExist() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE indexdef LIKE '%gin_trgm_ops%'", String.class);

        assertTrue(indexes.contains("idx_open_food_name_trgm"), indexes.toString());
        assertTrue(indexes.contains("idx_food_name_trgm"), indexes.toString());
    }

    @Test
    void testJitIsDisabled() {
        assertEquals("off", jdbc.queryForObject("SHOW jit", String.class));
    }

    @Test
    void testOpenFoodSearchUsesIndex() {
        jdbc.execute("SET enable_seqscan = off");
        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN SELECT id FROM open_food WHERE lower(name) % 'nutella'", String.class));

        assertTrue(plan.contains("idx_open_food_name_trgm"), plan);
    }
}

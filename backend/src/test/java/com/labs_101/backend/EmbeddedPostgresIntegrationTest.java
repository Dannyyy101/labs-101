package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.OpenFood;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;
import com.labs_101.backend.repositories.SearchFoodProjection;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
public class EmbeddedPostgresIntegrationTest {
    @Autowired
    private FoodRepository foodRepository;
    @Autowired
    private TrackedFoodRepository foodUserRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OpenFoodRepository openFoodRepository;

    @Test
    void testSearchByNameWithEqualName() {
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        Page<SearchFoodProjection> foods = foodRepository.search(Pageable.ofSize(5), "apfel", null);
        assertEquals(foods.stream().toList().size(), 1);
        assertEquals("Apfel", foods.getContent().getFirst().getName());
    }

    @Test
    void testSearchByNameWithLikeName() {
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        Page<SearchFoodProjection> foods = foodRepository.search(Pageable.ofSize(5), "pfe", null);
        assertEquals(foods.stream().toList().size(), 1);
        assertEquals("Apfel", foods.getContent().getFirst().getName());
    }

    @Test
    void testSearchByRecentUsage() {
        Food aubergine = new Food("Aubergine");
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        foodRepository.save(new Food("Ananas"));
        foodRepository.save(aubergine);
        User user = userRepository.save(new User("1"));
        foodUserRepository.save(new TrackedFood(null, user, aubergine, 1.0, null, null, null, null));

        Page<SearchFoodProjection> foods = foodRepository.search(Pageable.ofSize(5), "a", user.getId());
        assertEquals(foods.stream().toList().size(), 3);
        assertEquals("Aubergine", foods.getContent().getFirst().getName());
    }

    @Test
    void testSearchIncludesOpenFood() {
        foodRepository.save(new Food("Apfel roh"));
        openFoodRepository.save(new OpenFood(null, "1", "Apfelmus", 80.0, null, null, null, null, null, null,
                null, null));
        openFoodRepository.save(new OpenFood(null, "2", "Skyr Natur", 60.0, null, null, null, null, null, null,
                null, null));

        List<SearchFoodProjection> apfel = foodRepository.search(Pageable.ofSize(5), "apfel", null).getContent();
        assertEquals(2, apfel.size());
        assertEquals("Apfel roh", apfel.get(0).getName());
        assertFalse(apfel.get(0).getOpenFood());
        assertEquals("Apfelmus", apfel.get(1).getName());
        assertTrue(apfel.get(1).getOpenFood());

        List<SearchFoodProjection> skyr = foodRepository.search(Pageable.ofSize(5), "skyr", null).getContent();
        assertEquals(1, skyr.size());
        assertEquals("Skyr Natur", skyr.getFirst().getName());
        assertTrue(skyr.getFirst().getOpenFood());
    }

    @Test
    void testSearchHidesImportedOpenFood() {
        Food imported = new Food("Skyr Natur");
        imported.setBarCode("2");
        foodRepository.save(imported);
        openFoodRepository.save(new OpenFood(null, "2", "Skyr Natur", 60.0, null, null, null, null, null, null,
                null, null));

        List<SearchFoodProjection> skyr = foodRepository.search(Pageable.ofSize(5), "skyr", null).getContent();
        assertEquals(1, skyr.size());
        assertEquals(imported.getId(), skyr.getFirst().getId());
        assertFalse(skyr.getFirst().getOpenFood());
    }

    @Test
    void testSearchShowsEveryNameOnce() {
        Food own = foodRepository.save(new Food("Skyr Natur"));
        openFoodRepository.save(new OpenFood(null, "1", "Skyr natur", 60.0, null, null, null, null, null, null,
                null, null));
        openFoodRepository.save(new OpenFood(null, "2", "Skyr  Natur", 62.0, null, null, null, null, null, null,
                null, null));
        openFoodRepository.save(new OpenFood(null, "3", "Skyr Vanille", 80.0, null, null, null, null, null, null,
                null, null));
        openFoodRepository.save(new OpenFood(null, "4", "Skyr Vanille", 82.0, null, null, null, null, null, null,
                null, null));

        Page<SearchFoodProjection> skyr = foodRepository.search(Pageable.ofSize(5), "skyr", null);
        assertEquals(2, skyr.getTotalElements());
        assertEquals(2, skyr.getContent().size());
        assertEquals(own.getId(), skyr.getContent().get(0).getId());
        assertFalse(skyr.getContent().get(0).getOpenFood());
        assertEquals("Skyr Vanille", skyr.getContent().get(1).getName());

        // the count of a partial page comes from the count query
        assertEquals(2, foodRepository.search(Pageable.ofSize(1), "skyr", null).getTotalElements());
    }

    @Test
    void testSearchShowsOurFoodsBeforeOpenFood() {
        foodRepository.save(new Food("Joghurt mit Skyr Kulturen"));
        openFoodRepository.save(new OpenFood(null, "1", "Skyr", 60.0, null, null, null, null, null, null,
                null, null));

        List<SearchFoodProjection> skyr = foodRepository.search(Pageable.ofSize(5), "skyr", null).getContent();
        assertEquals(2, skyr.size());
        assertFalse(skyr.get(0).getOpenFood());
        assertEquals("Skyr", skyr.get(1).getName());
        assertTrue(skyr.get(1).getOpenFood());
    }
}

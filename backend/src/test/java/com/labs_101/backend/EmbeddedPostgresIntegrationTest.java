package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.EmbeddedPostgresConfiguration.EmbeddedPostgresExtension;
import com.labs_101.backend.entities.Food;
import com.labs_101.backend.entities.FoodUser;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.FoodUserRepository;
import com.labs_101.backend.repositories.UserRepository;

@DataJpaTest
@ExtendWith(EmbeddedPostgresExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
public class EmbeddedPostgresIntegrationTest {
    @Autowired
    private FoodRepository foodRepository;
    @Autowired
    private FoodUserRepository foodUserRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void testSearchByNameWithEqualName() {
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        Page<Food> foods = foodRepository.findAllByNameAndUserId(Pageable.ofSize(5), "Apfel", null);
        assertEquals(foods.stream().toList().size(), 1);
        Food first = foods.get().findFirst().orElse(new Food());
        assertEquals("Apfel", first.getName());
    }

    @Test
    void testSearchByNameWithLikeName() {
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        Page<Food> foods = foodRepository.findAllByNameAndUserId(Pageable.ofSize(5), "pfe", null);
        assertEquals(foods.stream().toList().size(), 1);
        Food first = foods.get().findFirst().orElse(new Food());
        assertEquals("Apfel", first.getName());
    }

    @Test
    void testSearchByRecentUsage() {
        Food aubergine = new Food("Aubergine");
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        foodRepository.save(new Food("Ananas"));
        foodRepository.save(aubergine);
        User user = userRepository.save(new User("1"));
        foodUserRepository.save(new FoodUser(null, user, aubergine, 1.0, "", null, null));

        Page<Food> foods = foodRepository.findAllByNameAndUserId(Pageable.ofSize(5), "A", user.getId());
        assertEquals(foods.stream().toList().size(), 3);
        Food first = foods.get().findFirst().orElse(new Food());
        assertEquals("Aubergine", first.getName());
    }
}
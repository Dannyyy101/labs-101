package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.EmbeddedPostgresConfiguration.EmbeddedPostgresExtension;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.entities.Food;
import com.labs_101.backend.entities.FoodUser;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.FoodUserRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.FoodService;

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
        List<Food> foods = foodRepository.findAllByNameAndUserId("Apfel", null);
        assertEquals(foods.size(), 1);
        assertEquals(foods.get(0).getName(), "Apfel");
    }

    @Test
    void testSearchByNameWithLikeName() {
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        List<Food> foods = foodRepository.findAllByNameAndUserId("pfe", null);
        assertEquals(foods.size(), 1);
        assertEquals(foods.get(0).getName(), "Apfel");
    }

    @Test
    void testSearchByRecentUsage() {
        Food aubergine = new Food("Aubergine");
        foodRepository.save(new Food("Birne"));
        foodRepository.save(new Food("Apfel"));
        foodRepository.save(new Food("Ananas"));
        foodRepository.save(aubergine);
        User user = userRepository.save(new User("1"));
        foodUserRepository.save(new FoodUser(null, user, aubergine, null, null));

        List<Food> foods = foodRepository.findAllByNameAndUserId("A", user.getId());
        assertEquals(foods.size(), 3);
        assertEquals("Aubergine", foods.get(0).getName());
    }
}
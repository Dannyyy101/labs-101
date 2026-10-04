package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.labs_101.backend.EmbeddedPostgresConfiguration.EmbeddedPostgresExtension;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;

@DataJpaTest
@ExtendWith(EmbeddedPostgresExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import(FoodExtractor.class)
public class EmbeddedPostgresFoodExtractorTest {
    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private FoodExtractor foodExtractor;

    @MockitoBean
    private FoodMapper foodMapper;

    @Test
    void testFindSimilar() {
        foodRepository.save(new Food("Apfel"));

        Food food = foodRepository.findSimilar("Apfel");
        assertEquals("Apfel", food.getName());

        foodRepository.save(new Food("Banane"));
        foodRepository.save(new Food("Ananas"));

        Food food2 = foodRepository.findSimilar("Banan");
        assertEquals("Banane", food2.getName());
    }

    @Test
    void testExtractFood() {
        foodRepository.save(new Food("Apfel"));
        Food cappucino = new Food("Cappucino");
        FoodPortion portion = new FoodPortion();
        portion.setLabel("Tasse");
        portion.setGrams(100.0);
        cappucino.addPortion(portion);
        foodRepository.save(cappucino);

        List<TrackedFoodDto> found = foodExtractor.extractFood("100 g Apfel");

        assertEquals(100, found.getFirst().amount());

        List<TrackedFoodDto> found2 = foodExtractor.extractFood("100 g Apfel eine Tasse Cappuccino");

        assertEquals(1, found2.get(1).amount());
        assertEquals("Tasse", found2.get(1).portion().label());
    }
}
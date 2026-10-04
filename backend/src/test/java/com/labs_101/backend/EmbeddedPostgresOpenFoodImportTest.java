package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.food.CreateFoodPortionDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodWithPortionsDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.OpenFood;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.FoodService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ FoodService.class, FoodMapper.class })
public class EmbeddedPostgresOpenFoodImportTest {
    @Autowired
    private FoodService foodService;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OpenFoodRepository openFoodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private OpenFood nutella() {
        return openFoodRepository.save(new OpenFood(null, "3017620422003", "Nutella", 539.0, null, 6.3, 30.9, 57.5,
                null, "Ferrero", null, null));
    }

    @Test
    void testImportCopiesOpenFoodIntoFoods() {
        OpenFood nutella = nutella();

        FoodWithPortionsDto imported = foodService.importOpenFood(nutella.getId());

        Food food = foodRepository.findById(imported.id()).orElseThrow();
        assertEquals("Nutella", food.getName());
        assertEquals("3017620422003", food.getBarCode());
        assertEquals(539.0, food.getKcal());
        assertEquals(6.3, food.getProtein());
        assertEquals(30.9, food.getFat());
        assertEquals(57.5, food.getCarbohydrates());
    }

    @Test
    void testImportTwiceDoesNotDuplicate() {
        OpenFood nutella = nutella();
        long before = foodRepository.count();

        FoodWithPortionsDto first = foodService.importOpenFood(nutella.getId());
        FoodWithPortionsDto second = foodService.importOpenFood(nutella.getId());

        assertEquals(first.id(), second.id());
        assertEquals(before + 1, foodRepository.count());
    }

    @Test
    void testImportUnknownOpenFood() {
        assertThrowsExactly(NotFoundException.class, () -> foodService.importOpenFood(-1L));
    }

    @Test
    void testTrackOpenFoodImportsAndTracks() {
        OpenFood nutella = nutella();
        User user = userRepository.save(new User("open-food-user"));

        TrackedFoodDto tracked = foodService.trackOpenFood(nutella.getId(),
                new CreateFoodUserDto(null, user.getId(), 20.0, "BREAKFAST", null));

        Food food = foodRepository.findByBarCode("3017620422003").orElseThrow();
        assertEquals(food.getId(), tracked.food().id());
        assertEquals(20.0, tracked.amount());

        // tracking again uses the imported food
        TrackedFoodDto trackedAgain = foodService.trackOpenFood(nutella.getId(),
                new CreateFoodUserDto(null, user.getId(), 15.0, "SNACK", null));
        assertEquals(food.getId(), trackedAgain.food().id());
    }

    @Test
    void testAddedPortionBelongsToFood() {
        FoodWithPortionsDto imported = foodService.importOpenFood(nutella().getId());

        foodService.createFoodPortion(imported.id(), new CreateFoodPortionDto("Esslöffel", 15.0, false));
        entityManager.flush();
        entityManager.clear();

        List<String> labels = foodRepository.findById(imported.id()).orElseThrow().getPortions().stream()
                .map((portion) -> portion.getLabel()).toList();
        assertEquals(List.of("Esslöffel"), labels);
    }
}

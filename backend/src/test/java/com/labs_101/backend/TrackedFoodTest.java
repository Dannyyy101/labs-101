package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.MealType;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.FoodService;

@ExtendWith(MockitoExtension.class)
public class TrackedFoodTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private TrackedFoodRepository foodUserRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FoodService foodService;

    private User user;
    private Food food;
    private TrackedFood trackedFood;

    @BeforeEach
    void generateTestData() {
        foodRepository.deleteAll();
        this.food = foodRepository.save(new Food());
        this.user = userRepository.save(new User());

        TrackedFood trackedFood = new TrackedFood(null, this.user, this.food, 1.0, MealType.BREAKFAST, null, null,
                null);
        this.trackedFood = foodUserRepository.save(trackedFood);

    }

    @Test
    void testUpdateTrackedFoodById() {
        assertThrows(NotFoundException.class, () -> foodService.updateTrackedFoodById(-1L, null));
    }

}

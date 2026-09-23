package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.UpdateFoodDto;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.services.FoodService;

@SpringBootTest
@Transactional
public class FoodTest {

    @Autowired
    private FoodService foodService;

    @Test
    void TestGetAll() {
        CreateFoodDto food1 = new CreateFoodDto("ABC", "Apple juice", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        CreateFoodDto food2 = new CreateFoodDto("DEF", "Orange juice", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        foodService.create(food1);
        foodService.create(food2);

        Page<FoodDto> page = foodService.getAll(null, Pageable.ofSize(2));
        assertEquals(2, page.getSize());
    }

    @Test
    void TestCreateFood() {
        CreateFoodDto food = new CreateFoodDto("ABC", "Apple juice", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        FoodDto response = foodService.create(food);
        assertEquals(food.blsCode(), response.blsCode());
        assertEquals(food.name(), response.name());
        assertEquals(food.kcal(), response.kcal());
        assertEquals(food.water(), response.water());
        assertEquals(food.protein(), response.protein());
        assertEquals(food.fat(), response.fat());
        assertEquals(food.carbohydrates(), response.carbohydrates());
        assertEquals(food.fiber(), response.fiber());
    }

    @Test
    void TestGetById() {
        CreateFoodDto food = new CreateFoodDto("ABC", "Apple juice", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        FoodDto entity = foodService.create(food);

        assertThrowsExactly(NotFoundException.class, () -> foodService.getById(-1L));

        FoodDto foodDto = foodService.getById(entity.id());

        assertEquals(food.blsCode(), foodDto.blsCode());
        assertEquals(food.name(), foodDto.name());
        assertEquals(food.kcal(), foodDto.kcal());
        assertEquals(food.water(), foodDto.water());
        assertEquals(food.protein(), foodDto.protein());
        assertEquals(food.fat(), foodDto.fat());
        assertEquals(food.carbohydrates(), foodDto.carbohydrates());
        assertEquals(food.fiber(), foodDto.fiber());
    }

    @Test
    void TestUpdateById() {
        CreateFoodDto food = new CreateFoodDto("ABC", "Apple juice", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);

        FoodDto entity = foodService.create(food);

        UpdateFoodDto updateFoodDto1 = new UpdateFoodDto(2L, "DEF", "Orange juice", 6.0, 5.0, 4.0, 3.0, 2.0, 1.0,
                new ArrayList<>());

        assertThrowsExactly(NotFoundException.class, () -> foodService.updateById(updateFoodDto1));

        UpdateFoodDto updateFoodDto2 = new UpdateFoodDto(1L, "DEF", "Orange juice", 6.0, 5.0, 4.0, 3.0, 2.0, 1.0,
                new ArrayList<>());

        FoodDto foodDto = foodService.updateById(updateFoodDto2);

        assertEquals(foodDto.blsCode(), updateFoodDto2.blsCode());
        assertEquals(foodDto.name(), updateFoodDto2.name());
        assertEquals(foodDto.kcal(), updateFoodDto2.kcal());
        assertEquals(foodDto.water(), updateFoodDto2.water());
        assertEquals(foodDto.protein(), updateFoodDto2.protein());
        assertEquals(foodDto.fat(), updateFoodDto2.fat());
        assertEquals(foodDto.carbohydrates(), updateFoodDto2.carbohydrates());
        assertEquals(foodDto.fiber(), updateFoodDto2.fiber());

    }
}

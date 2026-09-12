package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.FoodUserDto;
import com.labs_101.backend.entities.Food;
import com.labs_101.backend.entities.FoodUser;
import com.labs_101.backend.entities.User;

public class FoodMapper {
    public static Food mapFromCreateFoodDtoToEntity(CreateFoodDto dto) {
        return new Food(null, dto.blsCode(), dto.name(), dto.kcal(), dto.water(), dto.protein(), dto.fat(),
                dto.carbohydrates(), dto.fiber(), null, null, null);
    }

    public static FoodDto mapFromEntityToFoodDto(Food entity) {
        return new FoodDto(entity.getId(), entity.getBlsCode(), entity.getName(), entity.getKcal(), entity.getWater(),
                entity.getProtein(), entity.getFat(), entity.getCarbohydrates(), entity.getFiber());
    }

    public static FoodUser mapFromCreateFoodUserDto(CreateFoodUserDto dto) {
        return new FoodUser(null, new User(dto.userId()), new Food(dto.foodId()), dto.amount(), dto.meal(), null, null);
    }

    public static FoodUserDto mapFromFoodUser(FoodUser foodUser) {
        Food food = foodUser.getFood();
        return new FoodUserDto(foodUser.getFood().getId(), food.getBlsCode(), food.getName(), food.getKcal(),
                food.getWater(),
                food.getProtein(), food.getFat(),
                food.getCarbohydrates(), food.getFiber(), foodUser.getUser().getId(),
                foodUser.getAmount(), foodUser.getMeal());
    }
}

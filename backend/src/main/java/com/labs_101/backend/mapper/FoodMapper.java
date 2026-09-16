package com.labs_101.backend.mapper;

import java.util.ArrayList;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.CreateOpenFoodDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.FoodPortionDto;
import com.labs_101.backend.dtos.food.FoodUserDto;
import com.labs_101.backend.dtos.food.MealDto;
import com.labs_101.backend.dtos.food.SearchFoodResponseDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.FoodUser;
import com.labs_101.backend.entities.food.MealType;
import com.labs_101.backend.entities.food.OpenFood;

public class FoodMapper {
    public static Food mapFromCreateFoodDtoToEntity(CreateFoodDto dto) {
        return new Food(null, dto.blsCode(), dto.name(), dto.kcal(), dto.water(), dto.protein(), dto.fat(),
                dto.carbohydrates(), dto.fiber(), "", null, null, null, null);
    }

    public static FoodDto mapFromEntityToFoodDto(Food entity) {
        return new FoodDto(entity.getId(), entity.getBlsCode(), entity.getName(), entity.getKcal(), entity.getWater(),
                entity.getProtein(), entity.getFat(), entity.getCarbohydrates(), entity.getFiber(),
                entity.getPortions().stream().map((portion) -> mapFromFoodPortionToFoodPortionDto(portion)).toList());
    }

    public static FoodUser mapFromCreateFoodUserDto(CreateFoodUserDto dto) {
        FoodPortion portion = null;
        if (dto.portionId() != null) {
            new FoodPortion(dto.portionId());
        }
        return new FoodUser(null, new User(dto.userId()), new Food(dto.foodId()), dto.amount(),
                MealType.valueOf(dto.meal()), portion, null, null);
    }

    public static FoodUserDto mapFromFoodUser(FoodUser foodUser) {
        Food food = foodUser.getFood();
        return new FoodUserDto(foodUser.getFood().getId(), food.getBlsCode(), food.getName(), food.getKcal(),
                food.getWater(),
                food.getProtein(), food.getFat(),
                food.getCarbohydrates(), food.getFiber(), foodUser.getUser().getId(),
                foodUser.getAmount(), new MealDto(foodUser.getMeal().name(), foodUser.getMeal().messageKey()),
                mapFromFoodPortionToFoodPortionDto(foodUser.getPortion()));
    }

    public static SearchFoodResponseDto mapFromEntityToSearchFoodResponseDto(Food e) {
        return new SearchFoodResponseDto(e.getId(), e.getName());
    }

    public static FoodPortionDto mapFromFoodPortionToFoodPortionDto(FoodPortion portion) {
        if (portion == null)
            return null;
        return new FoodPortionDto(portion.getId(), portion.getGrams(), portion.getLabel(), portion.getIsDefault());
    }

    public static TrackedFoodDto mapFromFoodAndFoodUserToTrackedFoodDto(Food food, FoodUser lastEntry) {
        TrackedFoodDto.LastTrackedFoodEntryDto last = null;
        if (lastEntry != null)
            last = new TrackedFoodDto.LastTrackedFoodEntryDto(lastEntry.getAmount(),
                    new MealDto(lastEntry.getMeal().name(), lastEntry.getMeal().messageKey()));
        return new TrackedFoodDto(food.getId(), food.getBlsCode(), food.getName(), food.getKcal(),
                food.getWater(),
                food.getProtein(), food.getFat(),
                food.getCarbohydrates(), food.getFiber(), last,
                food.getPortions().stream().map((portion) -> mapFromFoodPortionToFoodPortionDto(portion)).toList());
    }

    public static OpenFood fromCreateOpenFoodDtoToOpenFood(CreateOpenFoodDto dto) {
        return new OpenFood(null, dto.barCode(), dto.name(), dto.kcal(), dto.water(), dto.protein(), dto.fat(),
                dto.carbohydrates(), dto.fiber(), dto.company(), null, null);
    }

    public static Food mapFromOpenFoodToFood(OpenFood openFood) {
        return new Food(null, null, openFood.getName(), openFood.getKcal(), openFood.getWater(), openFood.getProtein(),
                openFood.getFat(), openFood.getCarbohydrates(), openFood.getFiber(), openFood.getBarCode(), null, null,
                new ArrayList<>(),
                new ArrayList<>());
    }
}

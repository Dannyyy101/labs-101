package com.labs_101.backend.controller;

import com.labs_101.backend.services.FoodService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodPortionDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.SearchFoodResponseDto;
import com.labs_101.backend.dtos.food.UpdateFoodDto;
import com.labs_101.backend.exception.BadRequestException;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController()
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService foodService;

    FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping("")
    public ResponseEntity<Void> create(@RequestBody CreateFoodDto dto) {
        foodService.create(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("{id}/portions")
    public ResponseEntity<Void> addFoodPortions(@PathVariable Long id, @RequestBody CreateFoodPortionDto dto) {
        foodService.createFoodPortion(id, dto);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("")
    public Page<FoodDto> getAll(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return foodService.getAll(query, pageable);
    }

    @PutMapping("/{id}")
    public FoodDto updateFood(@PathVariable Long id, @RequestBody UpdateFoodDto dto) {
        return foodService.update(dto);
    }

    @GetMapping("/search/byNameAndUser")
    public Page<SearchFoodResponseDto> search(Pageable p, @RequestParam String name, @RequestParam String userId) {
        return foodService.searchByNameAndUserId(p, name, userId);
    }

    @PostMapping("{id}/track")
    public ResponseEntity<Void> trackFoodForUser(@PathVariable Long id, @RequestBody CreateFoodUserDto dto) {
        if (!id.equals(dto.foodId()))
            throw BadRequestException.workout(id, dto.foodId());

        foodService.trackFood(dto);

        return ResponseEntity.noContent().build();
    }
}

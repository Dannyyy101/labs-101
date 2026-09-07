package com.labs_101.backend.controller;

import com.labs_101.backend.services.FoodService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodDto;

import com.labs_101.backend.exception.BadRequestException;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("")
    public Page<FoodDto> getAll(Pageable p) {
        return foodService.getAll(p);
    }

    @GetMapping("/search")
    public Page<FoodDto> search(Pageable p, @RequestParam(required = false) String name) {
        if (name != null)
            return foodService.searchByName(p, name);

        return null;
    }

    @PostMapping("{id}/track")
    public ResponseEntity<Void> trackFoodForUser(@PathVariable Long id, @RequestBody CreateFoodUserDto dto) {
        if (!id.equals(dto.foodId()))
            throw BadRequestException.workout(id, dto.foodId());

        foodService.trackFood(dto);

        return ResponseEntity.noContent().build();
    }
}

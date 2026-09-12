package com.labs_101.backend.controller;

import com.labs_101.backend.services.FoodService;
import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodUserDto;

@RestController()
@RequestMapping("/api/users")
public class UserController {

    private final FoodService foodService;

    UserController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("{id}/tracked-food")
    public List<FoodUserDto> getTrackedFoodForUser(@PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant date) {
        return foodService.getTrackedFoodForUser(id, date);
    }
}

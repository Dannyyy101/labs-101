package com.labs_101.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodUserDto;
import com.labs_101.backend.dtos.food.FoodWithLastEntryAndPortionsDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.services.FoodService;

@RestController()
@RequestMapping("/api/users")
public class UserController {

    private final FoodService foodService;

    UserController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("{id}/tracked-foods")
    public List<TrackedFoodDto> getTrackedFoodForUser(@PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant date) {
        return foodService.getTrackedFoodForUser(id, date);
    }

    @DeleteMapping("/{userId}/tracked-foods/{trackedFoodId}")
    public ResponseEntity<Void> deleteTrackedFoodById(@PathVariable String userId, @PathVariable Long trackedFoodId) {
        foodService.deleteTrackedFoodById(userId, trackedFoodId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/tracked-foods/{trackedFoodId}")
    public ResponseEntity<Void> updateTrackedFoodById(@PathVariable String userId, @PathVariable Long trackedFoodId,
            @RequestBody CreateFoodUserDto dto) {
        foodService.updateTrackedFoodById(trackedFoodId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/tracked-foods/{trackedFoodId}")
    public TrackedFoodDto getTrackedFoodById(@PathVariable String userId, @PathVariable Long trackedFoodId) {
        return foodService.getTrackedFoodById(trackedFoodId);
    }

    @GetMapping("/{userId}/foods/{foodId}/last")
    public FoodWithLastEntryAndPortionsDto getLastTracked(@PathVariable String userId,
            @PathVariable Long foodId) {
        return foodService.getFoodWithLastEntry(userId, foodId);

    }
}

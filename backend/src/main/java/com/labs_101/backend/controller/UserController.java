package com.labs_101.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.dtos.user.UserProfileDto;
import com.labs_101.backend.services.FoodService;
import com.labs_101.backend.services.UserService;
import com.labs_101.backend.security.CurrentUser;

@RestController()
@RequestMapping("/api/users")
public class UserController {

    private final FoodService foodService;
    private final UserService userService;

    UserController(FoodService foodService, UserService userService) {
        this.foodService = foodService;
        this.userService = userService;
    }

    /** People to invite, matched by name or email. */
    @GetMapping("")
    public List<CalendarUserDto> searchUsers(@RequestParam String query) {
        return userService.search(query);
    }

    /** Name, email and profile picture, live from Zitadel where they are changed. */
    @GetMapping("/me")
    public UserProfileDto getProfile(@CurrentUser String userId, @AuthenticationPrincipal Jwt token) {
        return userService.getProfile(userId, token != null ? token.getTokenValue() : null);
    }

    @GetMapping("/me/tracked-foods")
    public List<TrackedFoodDto> getTrackedFoodForUser(@CurrentUser String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant date) {
        return foodService.getTrackedFoodForUser(id, date);
    }

    @DeleteMapping("/me/tracked-foods/{trackedFoodId}")
    public ResponseEntity<Void> deleteTrackedFoodById(@CurrentUser String userId, @PathVariable Long trackedFoodId) {
        foodService.deleteTrackedFoodById(userId, trackedFoodId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/tracked-foods/{trackedFoodId}")
    public ResponseEntity<Void> updateTrackedFoodById(@CurrentUser String userId, @PathVariable Long trackedFoodId,
            @RequestBody CreateFoodUserDto dto) {
        foodService.updateTrackedFoodById(trackedFoodId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/tracked-foods/{trackedFoodId}")
    public TrackedFoodDto getTrackedFoodById(@CurrentUser String userId, @PathVariable Long trackedFoodId) {
        return foodService.getTrackedFoodById(trackedFoodId);
    }

    @GetMapping("/me/foods/{foodId}/last")
    public FoodWithLastEntryAndPortionsDto getLastTracked(@CurrentUser String userId,
            @PathVariable Long foodId) {
        return foodService.getFoodWithLastEntry(userId, foodId);

    }
}

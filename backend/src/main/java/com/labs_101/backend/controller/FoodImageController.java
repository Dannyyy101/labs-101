package com.labs_101.backend.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.labs_101.backend.dtos.foodImage.AcceptFoodImageDto;
import com.labs_101.backend.dtos.foodImage.FoodImageAnalysisDto;
import com.labs_101.backend.dtos.foodImage.FoodImageUsageDto;
import com.labs_101.backend.entities.food.FoodImage;
import com.labs_101.backend.security.CurrentUser;
import com.labs_101.backend.services.FoodImageService;

/** Photos of meals, analyzed in the background, see {@link FoodImageService}. */
@RestController()
@RequestMapping("/api/users/me/food-images")
public class FoodImageController {
    static final int MAX_LIMIT = 50;

    private final FoodImageService foodImageService;

    FoodImageController(FoodImageService foodImageService) {
        this.foodImageService = foodImageService;
    }

    /**
     * 202: the photo is stored and its analysis started, the user gets a
     * notification once it is done. {@code description} is an optional hint
     * what is on the photo.
     */
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FoodImageAnalysisDto> upload(@CurrentUser String userId,
            @RequestPart("image") MultipartFile image, @RequestParam String meal,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(foodImageService.upload(userId, image, meal, description));
    }

    @GetMapping("")
    public List<FoodImageAnalysisDto> getAll(@CurrentUser String userId,
            @RequestParam(defaultValue = "10") int limit) {
        return foodImageService.getAll(userId, Math.clamp(limit, 1, MAX_LIMIT));
    }

    @GetMapping("/usage")
    public FoodImageUsageDto usage(@CurrentUser String userId) {
        return foodImageService.usage(userId);
    }

    @GetMapping("/{id}")
    public FoodImageAnalysisDto get(@CurrentUser String userId, @PathVariable Long id) {
        return foodImageService.get(userId, id);
    }

    // the photo never changes, so the browser may keep it
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@CurrentUser String userId, @PathVariable Long id) {
        FoodImage image = foodImageService.getImage(userId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(image.getData());
    }

    @PostMapping("/{id}/accept")
    public FoodImageAnalysisDto accept(@CurrentUser String userId, @PathVariable Long id,
            @RequestBody AcceptFoodImageDto dto) {
        return foodImageService.accept(userId, id, dto);
    }

    @PostMapping("/{id}/reject")
    public FoodImageAnalysisDto reject(@CurrentUser String userId, @PathVariable Long id) {
        return foodImageService.reject(userId, id);
    }
}

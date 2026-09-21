package com.labs_101.backend.services;

import com.labs_101.backend.repositories.FoodRepository;

import com.labs_101.backend.repositories.TrackedFoodRepository;

import com.labs_101.backend.repositories.OpenFoodRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodPortionDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.CreateOpenFoodDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.FoodUserDto;
import com.labs_101.backend.dtos.food.SearchFoodResponseDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.dtos.food.UpdateFoodDto;
import com.labs_101.backend.dtos.food.UpdateFoodPortionDto;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.entities.food.OpenFood;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.FoodMapper;

@Service
public class FoodService {
    private final OpenFoodRepository openFoodRepository;
    private final TrackedFoodRepository foodUserRepository;
    private final FoodRepository foodRepository;

    FoodService(FoodRepository foodRepository, TrackedFoodRepository foodUserRepository,
            OpenFoodRepository openFoodRepository) {
        this.foodRepository = foodRepository;
        this.foodUserRepository = foodUserRepository;
        this.openFoodRepository = openFoodRepository;
    }

    public void create(CreateFoodDto dto) {
        foodRepository.save(FoodMapper.mapFromCreateFoodDtoToEntity(dto));
    }

    public FoodDto getById(Long id) {
        Food entity = foodRepository.findById(id).orElseThrow(() -> NotFoundException.food(id));

        return FoodMapper.mapFromEntityToFoodDto(entity);
    }

    public Page<FoodDto> getAll(String query, Pageable p) {
        Page<Food> entities = (query == null || query.isBlank())
                ? foodRepository.findAll(p)
                : foodRepository.findByNameContainingIgnoreCase(query.trim(), p);

        return entities.map(FoodMapper::mapFromEntityToFoodDto);
    }

    public Page<SearchFoodResponseDto> searchByNameAndUserId(Pageable p, String name, String userId) {
        Page<Food> entities = foodRepository.findAllByNameAndUserId(p, name, userId);

        final Page<SearchFoodResponseDto> page = new PageImpl<>(
                entities.stream().map((e) -> FoodMapper.mapFromEntityToSearchFoodResponseDto(e)).toList(), p,
                entities.getSize());

        return page;
    }

    public void trackFood(CreateFoodUserDto dto) {
        foodUserRepository.save(FoodMapper.mapFromCreateFoodUserDto(dto));
    }

    public TrackedFoodDto updateTrackedFoodById(Long id, UpdateFoodPortionDto foodPortionDto) {
        TrackedFood trackedFood = foodUserRepository.findById(id).orElseThrow(() -> NotFoundException.trackedFood(id));
        if (foodPortionDto == null) {
            return FoodMapper.mapFromFoodAndFoodUserToTrackedFoodDto(trackedFood.getFood(), trackedFood);
        }

        return null;
    }

    @Transactional
    public FoodDto update(UpdateFoodDto dto) {
        Food food = foodRepository.findById(dto.id())
                .orElseThrow(() -> NotFoundException.food(dto.id()));

        if (dto.blsCode() != null)
            food.setBlsCode(dto.blsCode());
        if (dto.name() != null)
            food.setName(dto.name());
        if (dto.kcal() != null)
            food.setKcal(dto.kcal());
        if (dto.water() != null)
            food.setWater(dto.water());
        if (dto.protein() != null)
            food.setProtein(dto.protein());
        if (dto.fat() != null)
            food.setFat(dto.fat());
        if (dto.carbohydrates() != null)
            food.setCarbohydrates(dto.carbohydrates());
        if (dto.fiber() != null)
            food.setFiber(dto.fiber());

        if (dto.portions() != null) {
            syncPortions(food, dto.portions());
        }

        return FoodMapper.mapFromEntityToFoodDto(foodRepository.save(food));
    }

    public List<FoodUserDto> getTrackedFoodForUser(String id, Instant date) {
        LocalDate day = date.atZone(ZoneOffset.UTC).toLocalDate();
        Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TrackedFood> entries = foodUserRepository.findByUser_IdAndCreateDateBetween(id, start, end);

        return entries.stream()
                .map((foodUser) -> FoodMapper.mapFromTrackedFood(foodUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackedFoodDto getFoodWithLastEntry(String userId, Long foodId) {

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> NotFoundException.food(foodId));

        TrackedFood lastEntry = foodUserRepository
                .findFirstByUser_IdAndFood_IdOrderByCreateDateDesc(userId, foodId)
                .orElse(null);

        return FoodMapper.mapFromFoodAndFoodUserToTrackedFoodDto(food, lastEntry);
    }

    public void createFoodPortion(Long foodId, CreateFoodPortionDto dto) {
        Food entity = foodRepository.findById(foodId).orElseThrow(() -> NotFoundException.food(foodId));

        entity.addPortion(new FoodPortion(null, entity, new ArrayList<>(), dto.label(), dto.grams(), dto.isDefault()));
        foodRepository.save(entity);
    }

    private void syncPortions(Food food, List<UpdateFoodPortionDto> incoming) {
        Set<Long> keepIds = incoming.stream()
                .map(UpdateFoodPortionDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        food.getPortions().removeIf(p -> !keepIds.contains(p.getId()));

        Map<Long, FoodPortion> existing = food.getPortions().stream()
                .collect(Collectors.toMap(FoodPortion::getId, Function.identity()));

        for (UpdateFoodPortionDto p : incoming) {
            if (p.id() == null) {
                FoodPortion neu = new FoodPortion(null, food, new ArrayList<>(), p.label(), p.grams(), p.isDefault());
                food.addPortion(neu);
            } else {
                FoodPortion old = existing.get(p.id());
                if (old == null) {
                    throw NotFoundException.foodPortion(p.id());
                }
                setIfNotNull(p.grams(), old::setGrams);
                setIfNotNull(p.label(), old::setLabel);
            }
        }
    }

    private static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null)
            setter.accept(value);
    }

    public void createOpenFood(CreateOpenFoodDto dto) {
        openFoodRepository.save(FoodMapper.fromCreateOpenFoodDtoToOpenFood(dto));
    }

    public FoodDto getByBarcode(String code) {
        Optional<Food> food = foodRepository.findByBarCode(code);
        if (food.isPresent()) {
            return FoodMapper.mapFromEntityToFoodDto(food.get());
        }
        OpenFood openFood = openFoodRepository.findByBarCode(code)
                .orElseThrow(() -> NotFoundException.foodByBarcode(code));
        return FoodMapper.mapFromEntityToFoodDto(foodRepository.save(FoodMapper.mapFromOpenFoodToFood(openFood)));
    }
}

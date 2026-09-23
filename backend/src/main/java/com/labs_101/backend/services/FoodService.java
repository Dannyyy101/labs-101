package com.labs_101.backend.services;

import com.labs_101.backend.repositories.FoodRepository;

import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.utils.Utils;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodPortionDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.CreateOpenFoodDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.FoodWithLastEntryAndPortionsDto;
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
    private final TrackedFoodRepository trackedFoodRepository;
    private final FoodRepository foodRepository;
    private final FoodMapper foodMapper;

    private final Logger logger = LoggerFactory.getLogger(FoodService.class);

    FoodService(FoodRepository foodRepository, TrackedFoodRepository trackedFoodRepository,
            OpenFoodRepository openFoodRepository, FoodMapper foodMapper) {
        this.foodRepository = foodRepository;
        this.trackedFoodRepository = trackedFoodRepository;
        this.openFoodRepository = openFoodRepository;
        this.foodMapper = foodMapper;
    }

    @Transactional(readOnly = true)
    public Page<FoodDto> getAll(String query, Pageable p) {
        Page<Food> entities = (query == null || query.isBlank())
                ? foodRepository.findAll(p)
                : foodRepository.findByNameContainingIgnoreCase(query.trim(), p);

        return entities.map(foodMapper::mapFromEntityToFoodDto);
    }

    public FoodDto create(CreateFoodDto dto) {
        Food entity = foodRepository.save(foodMapper.mapFromCreateFoodDtoToEntity(dto));
        logger.info("Food was created successfully with id: " + entity.getId());
        return foodMapper.mapFromEntityToFoodDto(entity);
    }

    @Transactional(readOnly = true)
    public FoodDto getById(Long id) {
        Food entity = foodRepository.findById(id).orElseThrow(() -> NotFoundException.food(id));

        return foodMapper.mapFromEntityToFoodDto(entity);
    }

    @Transactional
    public FoodDto updateById(UpdateFoodDto dto) {
        Food food = foodRepository.findById(dto.id())
                .orElseThrow(() -> NotFoundException.food(dto.id()));

        Utils.setIfNotNull(dto.blsCode(), food::setBlsCode);
        Utils.setIfNotNull(dto.name(), food::setName);
        Utils.setIfNotNull(dto.kcal(), food::setKcal);
        Utils.setIfNotNull(dto.water(), food::setWater);
        Utils.setIfNotNull(dto.protein(), food::setProtein);
        Utils.setIfNotNull(dto.fat(), food::setFat);
        Utils.setIfNotNull(dto.carbohydrates(), food::setCarbohydrates);
        Utils.setIfNotNull(dto.fiber(), food::setFiber);

        if (dto.portions() != null) {
            syncPortions(food, dto.portions());
        }

        return foodMapper.mapFromEntityToFoodDto(foodRepository.save(food));
    }

    @Transactional(readOnly = true)
    public Page<SearchFoodResponseDto> searchByNameAndUserId(Pageable p, String name, String userId) {
        Page<Food> entities = null;
        if (name == null || name.isBlank()) {
            entities = foodRepository.findAllByLastUsed(p, userId);
        } else {
            entities = foodRepository.findAllByNameAndUserId(p, name, userId);
        }

        return entities.map(FoodMapper::mapFromEntityToSearchFoodResponseDto);
    }

    public TrackedFoodDto trackFood(CreateFoodUserDto dto) {
        TrackedFood entity = trackedFoodRepository.save(FoodMapper.mapFromCreateFoodUserDto(dto));
        return foodMapper.mapFromTrackedFood(entity);
    }

    @Transactional(readOnly = true)
    public List<TrackedFoodDto> getTrackedFoodForUser(String id, Instant date) {
        LocalDate day = date.atZone(ZoneOffset.UTC).toLocalDate();
        Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TrackedFood> entries = trackedFoodRepository.findByUser_IdAndCreateDateBetween(id, start, end);

        return entries.stream()
                .map((foodUser) -> foodMapper.mapFromTrackedFood(foodUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public FoodWithLastEntryAndPortionsDto getFoodWithLastEntry(String userId, Long foodId) {

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> NotFoundException.food(foodId));

        TrackedFood lastEntry = trackedFoodRepository
                .findFirstByUser_IdAndFood_IdOrderByCreateDateDesc(userId, foodId)
                .orElse(null);

        return foodMapper.mapFromFoodAndTrackedFoodToFoodWithLastEntryDto(food, lastEntry);
    }

    public void createFoodPortion(Long foodId, CreateFoodPortionDto dto) {
        Food entity = foodRepository.findById(foodId).orElseThrow(() -> NotFoundException.food(foodId));

        entity.addPortion(foodMapper.mapFromCreateFoodPortionDtoToFoodPortion(dto));
        foodRepository.save(entity);
    }

    public void createOpenFood(CreateOpenFoodDto dto) {
        openFoodRepository.save(FoodMapper.fromCreateOpenFoodDtoToOpenFood(dto));
    }

    public FoodDto getByBarcode(String code) {
        Optional<Food> food = foodRepository.findByBarCode(code);
        if (food.isPresent()) {
            return foodMapper.mapFromEntityToFoodDto(food.get());
        }
        OpenFood openFood = openFoodRepository.findByBarCode(code)
                .orElseThrow(() -> NotFoundException.foodByBarcode(code));
        return foodMapper.mapFromEntityToFoodDto(foodRepository.save(FoodMapper.mapFromOpenFoodToFood(openFood)));
    }

    public void deleteTrackedFoodById(String userId, Long trackedFoodId) {
        trackedFoodRepository.deleteById(trackedFoodId);
    }

    @Transactional
    public TrackedFoodDto updateTrackedFoodById(Long id, CreateFoodUserDto trackedFood) {
        TrackedFood food = trackedFoodRepository.findById(id).orElseThrow(() -> NotFoundException.trackedFood(id));
        Utils.setIfNotNull(trackedFood.amount(), food::setAmount);
        if (trackedFood.portionId() != null) {
            food.setPortion(new FoodPortion(trackedFood.portionId()));
        }
        return foodMapper.mapFromTrackedFood(trackedFoodRepository.save(food));
    }

    public TrackedFoodDto getTrackedFoodById(Long trackedFoodId) {
        TrackedFood food = trackedFoodRepository.findById(trackedFoodId)
                .orElseThrow(() -> NotFoundException.trackedFood(trackedFoodId));
        return foodMapper.mapFromTrackedFood(food);
    }

    private void syncPortions(Food food, List<UpdateFoodPortionDto> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            if (food.getPortions() != null) {
                food.getPortions().clear();
            }
            return;
        }

        Set<Long> keepIds = incoming.stream()
                .map(UpdateFoodPortionDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        food.getPortions().removeIf(p -> !keepIds.contains(p.getId()));

        Map<Long, FoodPortion> existing = food.getPortions().stream()
                .collect(Collectors.toMap(FoodPortion::getId, Function.identity()));

        for (UpdateFoodPortionDto p : incoming) {
            if (p.id() == null) {
                FoodPortion newPortion = new FoodPortion(null, food, new ArrayList<>(), p.label(), p.grams(),
                        p.isDefault());
                food.addPortion(newPortion);
            } else {
                FoodPortion old = existing.get(p.id());
                if (old == null) {
                    throw NotFoundException.foodPortion(p.id());
                }
                Utils.setIfNotNull(p.grams(), old::setGrams);
                Utils.setIfNotNull(p.label(), old::setLabel);
                Utils.setIfNotNull(p.isDefault(), old::setIsDefault);
            }
        }
    }

}

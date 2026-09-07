package com.labs_101.backend.services;

import com.labs_101.backend.repositories.FoodRepository;

import com.labs_101.backend.repositories.FoodUserRepository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.food.CreateFoodDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.entities.Food;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.FoodMapper;

@Service
public class FoodService {
    private final FoodUserRepository foodUserRepository;
    private final FoodRepository foodRepository;

    FoodService(FoodRepository foodRepository, FoodUserRepository foodUserRepository) {
        this.foodRepository = foodRepository;
        this.foodUserRepository = foodUserRepository;
    }

    public void create(CreateFoodDto dto) {
        foodRepository.save(FoodMapper.mapFromCreateFoodDtoToEntity(dto));
    }

    public FoodDto getById(Long id) {
        Food entity = foodRepository.findById(id).orElseThrow(() -> NotFoundException.food(id));

        return FoodMapper.mapFromEntityToFoodDto(entity);
    }

    public Page<FoodDto> getAll(Pageable p) {
        Page<Food> entities = foodRepository.findAll(p);

        final Page<FoodDto> page = new PageImpl<>(
                entities.stream().map((e) -> FoodMapper.mapFromEntityToFoodDto(e)).toList(), p,
                entities.getSize());

        return page;
    }

    public Page<FoodDto> searchByName(Pageable p, String name) {
        Page<Food> entities = foodRepository.findAll(p);

        final Page<FoodDto> page = new PageImpl<>(
                entities.stream().map((e) -> FoodMapper.mapFromEntityToFoodDto(e)).toList(), p,
                entities.getSize());

        return page;
    }

    public void trackFood(CreateFoodUserDto dto) {
        foodUserRepository.save(FoodMapper.mapFromCreateFoodUserDto(dto));
    }
}

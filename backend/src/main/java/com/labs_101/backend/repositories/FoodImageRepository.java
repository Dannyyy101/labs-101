package com.labs_101.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.food.FoodImage;

public interface FoodImageRepository extends JpaRepository<FoodImage, Long> {
}

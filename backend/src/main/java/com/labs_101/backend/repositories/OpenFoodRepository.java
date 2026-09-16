package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.food.OpenFood;

public interface OpenFoodRepository extends JpaRepository<OpenFood, Long> {
    Optional<OpenFood> findByBarCode(String barcode);
}

package com.labs_101.backend.entities.food;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "food")
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String blsCode;
    private String name;
    private Double kcal;
    private Double water;
    private Double protein;
    private Double fat;
    private Double carbohydrates;
    private Double fiber;

    private String barCode;

    @CreationTimestamp
    private Instant createDate;

    @UpdateTimestamp
    private Instant updateDate;

    @OneToMany(mappedBy = "food")
    private List<TrackedFood> foodUsers;

    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodPortion> portions = new ArrayList<>();

    public Food(Long foodId) {
        this.id = foodId;
    }

    public Food(String name) {
        this.name = name;
    }

    // no @AllArgsConstructor: it would assign portions, which already has an initializer
    /** Nutrients per 100 g. */
    public Food(String blsCode, String name, Double kcal, Double water, Double protein, Double fat,
            Double carbohydrates, Double fiber, String barCode) {
        this.blsCode = blsCode;
        this.name = name;
        this.kcal = kcal;
        this.water = water;
        this.protein = protein;
        this.fat = fat;
        this.carbohydrates = carbohydrates;
        this.fiber = fiber;
        this.barCode = barCode;
    }

    public void addPortion(FoodPortion portion) {
        portion.setFood(this);
        this.portions.add(portion);
    }
}

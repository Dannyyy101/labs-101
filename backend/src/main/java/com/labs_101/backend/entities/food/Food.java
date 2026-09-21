package com.labs_101.backend.entities.food;

import java.time.Instant;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
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
    private List<FoodPortion> portions;

    public Food(Long foodId) {
        this.id = foodId;
    }

    public Food(String name) {
        this.name = name;
    }

    public void addPortion(FoodPortion portion) {
        this.portions.add(portion);
    }
}

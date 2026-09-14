package com.labs_101.backend.entities.food;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "food_portion")
@Entity
public class FoodPortion {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Food food;

    @OneToMany(mappedBy = "portion")
    private List<FoodUser> foodUser;

    private String label;
    private Double grams;
    private Boolean isDefault;

    public FoodPortion(Long id) {
        this.id = id;
    }
}

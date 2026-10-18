package com.labs_101.backend.entities.food;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
    private List<TrackedFood> foodUser;

    private String label;
    private Double grams;
    private Boolean isDefault = false;

    public FoodPortion(Long id) {
        this.id = id;
    }

    // no @AllArgsConstructor: it would assign isDefault, which already has an initializer
    /** {@code food} may be null when it is set by {@link Food#addPortion} afterwards. */
    public FoodPortion(Food food, String label, Double grams, Boolean isDefault) {
        this.food = food;
        this.label = label;
        this.grams = grams;
        this.isDefault = isDefault;
    }
}

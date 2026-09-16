package com.labs_101.backend.entities.food;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "open_food", uniqueConstraints = @UniqueConstraint(columnNames = "bar_code"), indexes = @Index(name = "idx_open_food_bar_code", columnList = "bar_code"))
public class OpenFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String barCode;
    private String name;
    private Double kcal;
    private Double water;
    private Double protein;
    private Double fat;
    private Double carbohydrates;
    private Double fiber;
    private String company;

    @CreationTimestamp
    private Instant createDate;

    @UpdateTimestamp
    private Instant updateDate;

}
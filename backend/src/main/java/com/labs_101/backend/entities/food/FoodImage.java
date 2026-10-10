package com.labs_101.backend.entities.food;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The photo of a {@link FoodImageAnalysis}, kept after the review. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "food_image")
public class FoodImage {
    /** same id as the analysis */
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FoodImageAnalysis analysis;

    @Column(nullable = false, length = 32)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public FoodImage(FoodImageAnalysis analysis, String contentType, byte[] data) {
        this.analysis = analysis;
        this.contentType = contentType;
        this.data = data;
    }
}

package com.labs_101.backend.entities.food;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import com.labs_101.backend.entities.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A photo of a meal, analyzed by the {@code FoodImageJob}. The photo itself is
 * in {@link FoodImage} so listing analyses doesn't load the bytes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "food_image_analysis", indexes = {
        @Index(name = "idx_food_image_analysis_user_created", columnList = "user_id, createdAt"),
        @Index(name = "idx_food_image_analysis_status", columnList = "status, nextAttemptAt") })
public class FoodImageAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // deleted together with the user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** meal chosen when uploading, the user can still change it when accepting */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MealType meal;

    /** optional hint of the user what is on the photo, e.g. "Spaghetti Bolognese mit Parmesan" */
    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FoodImageStatus status = FoodImageStatus.PENDING;

    /** failed calls to the model, the job gives up after {@code FoodImageService.MAX_ATTEMPTS} */
    @Column(nullable = false)
    private int attempts;

    /** when the job tries again after a failed attempt, also used to continue after a restart */
    @Column(nullable = false)
    private Instant nextAttemptAt;

    /** last error of the job, for the logs and the user */
    @Column(columnDefinition = "text")
    private String error;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Item> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    private Instant analyzedAt;

    /** accepted or rejected */
    private Instant reviewedAt;

    /**
     * A food the model saw on the photo. {@code foodId} or {@code openFoodId} is
     * the best match in our databases, both are null if there is none.
     *
     * @param unit         "g", "ml" or a portion like "Stück", "Scheibe"
     * @param gramsPerUnit estimated by the model, 1 for g and ml
     */
    public record Item(String name, Double amount, String unit, Double gramsPerUnit, Long foodId, Long openFoodId) {
    }
}

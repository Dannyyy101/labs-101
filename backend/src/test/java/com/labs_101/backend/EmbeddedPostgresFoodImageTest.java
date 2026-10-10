package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.labs_101.backend.dtos.foodImage.AcceptFoodImageDto;
import com.labs_101.backend.dtos.foodImage.FoodImageAnalysisDto;
import com.labs_101.backend.dtos.foodImage.FoodImageItemDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodImageStatus;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.MealType;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.TooManyRequestsException;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.foodImage.GeminiClient;
import com.labs_101.backend.foodImage.GeminiClient.DetectedFood;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodImageAnalysisRepository;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.FoodImageService;
import com.labs_101.backend.services.FoodService;
import com.labs_101.backend.services.NotificationService;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ FoodImageService.class, FoodService.class, FoodExtractor.class, FoodMapper.class,
        NotificationService.class })
public class EmbeddedPostgresFoodImageTest {
    private static final String USER_ID = "photo-user";
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3 };

    @MockitoBean
    private GeminiClient geminiClient;

    @Autowired
    private FoodImageService foodImageService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodRepository foodRepository;
    @Autowired
    private TrackedFoodRepository trackedFoodRepository;
    @Autowired
    private FoodImageAnalysisRepository analysisRepository;
    @Autowired
    private EntityManager entityManager;

    private Food banana;
    private Food rice;

    @BeforeEach
    void setUp() {
        when(geminiClient.isConfigured()).thenReturn(true);
        userRepository.save(new User(USER_ID));

        banana = new Food("Banane roh");
        banana.setKcal(90.0);
        banana = foodRepository.save(banana);

        rice = new Food("Reis gekocht");
        rice.setKcal(130.0);
        FoodPortion bowl = new FoodPortion();
        bowl.setLabel("Schüssel");
        bowl.setGrams(250.0);
        rice.addPortion(bowl);
        rice = foodRepository.save(rice);
        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private FoodImageAnalysisDto upload() {
        return upload(null);
    }

    private FoodImageAnalysisDto upload(String description) {
        return foodImageService.upload(USER_ID, new MockMultipartFile("image", "meal.jpg", "image/jpeg", JPEG),
                "lunch", description);
    }

    private List<Long> pendingIds() {
        return foodImageService.findPending().stream().map((pending) -> pending.id()).toList();
    }

    private FoodImageAnalysisDto analyzed(List<DetectedFood> detected) {
        when(geminiClient.detect(any(), anyString(), any())).thenReturn(detected);
        FoodImageAnalysisDto uploaded = upload();
        foodImageService.analyze(uploaded.id());
        flushAndClear();
        return foodImageService.get(USER_ID, uploaded.id());
    }

    @Test
    void testUploadIsPendingAndKeepsThePhoto() {
        FoodImageAnalysisDto uploaded = upload();
        flushAndClear();

        assertEquals(FoodImageStatus.PENDING, uploaded.status());
        assertEquals(MealType.LUNCH, uploaded.meal());
        assertEquals(List.of(uploaded.id()), pendingIds());
        assertNull(uploaded.description());
        assertArrayEquals(JPEG, foodImageService.getImage(USER_ID, uploaded.id()).getData());
        assertEquals("image/jpeg", foodImageService.getImage(USER_ID, uploaded.id()).getContentType());
    }

    @Test
    void testAnalyzeMatchesFoodsAndNotifies() {
        FoodImageAnalysisDto analysis = analyzed(List.of(
                new DetectedFood("Banane", 1.0, "Stück", 120.0),
                new DetectedFood("Reis gekocht", 180.0, "G", 5.0),
                new DetectedFood("Drachenfruchtsalat mit Einhornstaub", 1.0, "Portion", 150.0)));

        assertEquals(FoodImageStatus.READY, analysis.status());
        FoodImageItemDto first = analysis.items().get(0);
        assertEquals(banana.getId(), first.food().id());
        assertEquals("Stück", first.unit());
        assertEquals(120, first.gramsPerUnit());
        // grams don't need an estimate per unit
        FoodImageItemDto second = analysis.items().get(1);
        assertEquals(rice.getId(), second.food().id());
        assertEquals("g", second.unit());
        assertEquals(1, second.gramsPerUnit());
        assertNull(analysis.items().get(2).food());
        assertTrue(pendingIds().isEmpty());
        verify(geminiClient).detect(any(), eq("image/jpeg"), isNull());

        NotificationDto notification = notificationService.getNotifications(USER_ID, true, null).getFirst();
        assertEquals(NotificationType.SUCCESS, notification.getType());
        assertEquals("/foods/images/" + analysis.id(), notification.getLink());
        assertEquals("Prüfen", notification.getLinkLabel());
    }

    @Test
    void testAcceptTracksFoodsAndAddsMissingPortions() {
        FoodImageAnalysisDto analysis = analyzed(List.of(
                new DetectedFood("Banane", 2.0, "Stück", 120.0),
                new DetectedFood("Reis gekocht", 1.0, "Schüssel", 300.0)));

        FoodImageAnalysisDto accepted = foodImageService.accept(USER_ID, analysis.id(), new AcceptFoodImageDto(
                "DINNER", List.of(
                        new AcceptFoodImageDto.Item(banana.getId(), null, 2.0, "Stück", 110.0),
                        new AcceptFoodImageDto.Item(rice.getId(), null, 1.0, "Schüssel", 300.0),
                        new AcceptFoodImageDto.Item(rice.getId(), null, 50.0, "g", 1.0))));
        flushAndClear();

        assertEquals(FoodImageStatus.ACCEPTED, accepted.status());
        assertEquals(MealType.DINNER, accepted.meal());

        // new unit, added as portion with the grams the user checked
        FoodPortion piece = foodRepository.findById(banana.getId()).orElseThrow().getPortions().getFirst();
        assertEquals("Stück", piece.getLabel());
        assertEquals(110, piece.getGrams());
        // existing unit, its grams stay
        Food storedRice = foodRepository.findById(rice.getId()).orElseThrow();
        assertEquals(1, storedRice.getPortions().size());
        assertEquals(250, storedRice.getPortions().getFirst().getGrams());

        List<TrackedFood> tracked = trackedFoodRepository.findAll().stream()
                .filter((t) -> t.getUser().getId().equals(USER_ID)).toList();
        assertEquals(3, tracked.size());
        assertTrue(tracked.stream().allMatch((t) -> t.getMeal() == MealType.DINNER));
        TrackedFood grams = tracked.stream().filter((t) -> t.getPortion() == null).findFirst().orElseThrow();
        assertEquals(50, grams.getAmount());

        // reviewed once only
        assertThrows(BadRequestException.class, () -> foodImageService.reject(USER_ID, analysis.id()));
    }

    @Test
    void testRejectKeepsThePhoto() {
        FoodImageAnalysisDto analysis = analyzed(List.of(new DetectedFood("Banane", 1.0, "Stück", 120.0)));

        assertEquals(FoodImageStatus.REJECTED, foodImageService.reject(USER_ID, analysis.id()).status());
        flushAndClear();
        assertArrayEquals(JPEG, foodImageService.getImage(USER_ID, analysis.id()).getData());
        assertThrows(BadRequestException.class, () -> foodImageService.accept(USER_ID, analysis.id(),
                new AcceptFoodImageDto(null, List.of())));
    }

    @Test
    void testDailyLimitWithoutFailedPhotos() {
        for (int i = 0; i < 5; i++)
            upload();
        assertThrows(TooManyRequestsException.class, this::upload);
        assertEquals(5, foodImageService.usage(USER_ID).used());

        // a failed photo gives the slot back
        when(geminiClient.detect(any(), anyString(), any())).thenThrow(new IllegalStateException("429 Too Many Requests"));
        Long id = pendingIds().getFirst();
        for (int attempt = 1; attempt <= 3; attempt++) {
            // the last attempt gives up, there is nothing to schedule
            assertEquals(attempt < 3, foodImageService.analyze(id).isPresent());
            flushAndClear();
        }
        assertEquals(FoodImageStatus.FAILED, foodImageService.get(USER_ID, id).status());
        assertEquals(4, foodImageService.usage(USER_ID).used());
        upload();
    }

    @Test
    void testFailedAttemptIsRetriedLater() {
        when(geminiClient.detect(any(), anyString(), any())).thenThrow(new IllegalStateException("503 Service Unavailable"));
        FoodImageAnalysisDto uploaded = upload();

        Optional<Instant> retryAt = foodImageService.analyze(uploaded.id());
        flushAndClear();

        assertEquals(FoodImageStatus.PENDING, foodImageService.get(USER_ID, uploaded.id()).status());
        assertEquals(1, analysisRepository.findById(uploaded.id()).orElseThrow().getAttempts());
        // waits a minute before the next attempt
        assertTrue(retryAt.orElseThrow().isAfter(Instant.now().plusSeconds(50)));
        assertEquals(retryAt.get(), foodImageService.findPending().getFirst().nextAttemptAt());
        assertEquals(0, notificationService.countUnread(USER_ID));
    }

    @Test
    void testDescriptionIsPassedToTheModel() {
        when(geminiClient.detect(any(), anyString(), any())).thenReturn(List.of());
        FoodImageAnalysisDto uploaded = upload("  Reis mit Hähnchen-Curry  ");
        foodImageService.analyze(uploaded.id());
        flushAndClear();

        assertEquals("Reis mit Hähnchen-Curry", foodImageService.get(USER_ID, uploaded.id()).description());
        verify(geminiClient).detect(any(), eq("image/jpeg"), eq("Reis mit Hähnchen-Curry"));
        assertThrows(BadRequestException.class, () -> upload("x".repeat(501)));
    }

    @Test
    void testUploadValidation() {
        assertThrows(BadRequestException.class, () -> foodImageService.upload(USER_ID,
                new MockMultipartFile("image", "a.gif", "image/gif", JPEG), "LUNCH", null));
        assertThrows(BadRequestException.class, () -> foodImageService.upload(USER_ID,
                new MockMultipartFile("image", "a.jpg", "image/jpeg", JPEG), "BRUNCH", null));
        when(geminiClient.isConfigured()).thenReturn(false);
        assertThrows(BadRequestException.class, this::upload);
    }
}

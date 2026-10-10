package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

import com.labs_101.backend.dtos.foodImage.FoodImageAnalysisDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.FoodImageStatus;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.foodImage.FoodImageJob;
import com.labs_101.backend.foodImage.GeminiClient;
import com.labs_101.backend.foodImage.GeminiClient.DetectedFood;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodImageAnalysisRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.FoodImageService;
import com.labs_101.backend.services.FoodService;
import com.labs_101.backend.services.NotificationService;

/** The job starts once the upload is committed, without waiting for anything else. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ FoodImageJob.class, FoodImageService.class, FoodService.class, FoodExtractor.class, FoodMapper.class,
        NotificationService.class })
public class EmbeddedPostgresFoodImageJobTest {
    @MockitoBean
    private GeminiClient geminiClient;

    @Autowired
    private FoodImageService foodImageService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodImageAnalysisRepository analysisRepository;

    @Test
    void testAnalysisStartsAfterCommit() {
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.detect(any(), anyString(), any()))
                .thenReturn(List.of(new DetectedFood("Banane", 1.0, "Stück", 120.0)));
        userRepository.save(new User("job-user"));
        FoodImageAnalysisDto uploaded = foodImageService.upload("job-user",
                new MockMultipartFile("image", "meal.jpg", "image/jpeg", new byte[] { 1, 2, 3 }), "SNACK",
                "eine Banane");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        verify(geminiClient, timeout(5000)).detect(any(), anyString(), any());
        long until = System.currentTimeMillis() + 5000;
        while (analysisRepository.findById(uploaded.id()).orElseThrow().getStatus() == FoodImageStatus.PENDING
                && System.currentTimeMillis() < until)
            Thread.onSpinWait();
        assertEquals(FoodImageStatus.READY, analysisRepository.findById(uploaded.id()).orElseThrow().getStatus());
    }
}

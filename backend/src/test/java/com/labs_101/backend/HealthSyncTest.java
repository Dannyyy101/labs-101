package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.dtos.health.AckHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.CompleteFullSyncDto;
import com.labs_101.backend.dtos.health.CreateHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.DeleteHealthSamplesDto;
import com.labs_101.backend.dtos.health.HealthSampleDto;
import com.labs_101.backend.dtos.health.HealthSampleKind;
import com.labs_101.backend.dtos.health.HealthWriteRequestDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestStatus;
import com.labs_101.backend.dtos.health.UploadHealthSamplesDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.HealthSampleRepository;
import com.labs_101.backend.repositories.HealthWriteRequestRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.HealthService;

import tools.jackson.databind.json.JsonMapper;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ HealthService.class, HealthSampleRepository.class, HealthWriteRequestRepository.class })
public class HealthSyncTest {
    private static final String HEART_RATE = "HKQuantityTypeIdentifierHeartRate";
    private static final String STEPS = "HKQuantityTypeIdentifierStepCount";

    @Autowired
    private HealthService healthService;
    @Autowired
    private UserRepository userRepository;

    private String userId;

    @BeforeEach
    void createUser() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
    }

    @Test
    void testUploadingTheSameSampleTwiceUpdatesIt() {
        UUID uuid = UUID.randomUUID();
        healthService.upload(userId, new UploadHealthSamplesDto(null, List.of(heartRate(uuid, 60))));
        healthService.upload(userId, new UploadHealthSamplesDto(null, List.of(heartRate(uuid, 72))));

        List<HealthSampleDto> samples = healthService.getSamples(userId, HEART_RATE, null, null, 0, 10);
        assertEquals(1, samples.size());
        assertEquals(72, samples.getFirst().value());
        assertEquals("Apple Watch", samples.getFirst().payload().get("device").get("name").asString());
    }

    @Test
    void testDeleteByUuid() {
        UUID kept = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();
        healthService.upload(userId, new UploadHealthSamplesDto(null, List.of(heartRate(kept, 60), heartRate(deleted, 61))));

        assertEquals(1, healthService.delete(userId, new DeleteHealthSamplesDto(List.of(deleted, UUID.randomUUID()))).count());
        assertEquals(List.of(kept), healthService.getSamples(userId, null, null, null, 0, 10).stream().map(HealthSampleDto::uuid).toList());
    }

    @Test
    void testCompletingAFullSyncRemovesSamplesItDidNotUpload() {
        UUID stillInHealthKit = UUID.randomUUID();
        UUID deletedWhileUninstalled = UUID.randomUUID();
        UUID otherType = UUID.randomUUID();
        healthService.upload(userId, new UploadHealthSamplesDto(null,
                List.of(heartRate(stillInHealthKit, 60), heartRate(deletedWhileUninstalled, 61), steps(otherType))));

        // the app was reinstalled and syncs heart rate from the beginning, in two batches
        UUID run = UUID.randomUUID();
        UUID newSample = UUID.randomUUID();
        healthService.upload(userId, new UploadHealthSamplesDto(run, List.of(heartRate(stillInHealthKit, 60))));
        healthService.upload(userId, new UploadHealthSamplesDto(run, List.of(heartRate(newSample, 65))));
        assertEquals(1, healthService.completeFullSync(userId, new CompleteFullSyncDto(HEART_RATE, run)).count());

        List<UUID> remaining = healthService.getSamples(userId, null, null, null, 0, 10).stream()
                .map(HealthSampleDto::uuid).toList();
        assertEquals(3, remaining.size());
        assertEquals(false, remaining.contains(deletedWhileUninstalled));
        // other types are untouched by the heart rate run
        assertEquals(true, remaining.contains(otherType));
    }

    @Test
    void testSamplesOfOtherUsersAreNeverChanged() {
        String otherUser = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
        UUID uuid = UUID.randomUUID();
        healthService.upload(otherUser, new UploadHealthSamplesDto(null, List.of(heartRate(uuid, 60))));

        healthService.upload(userId, new UploadHealthSamplesDto(null, List.of(heartRate(uuid, 99))));
        healthService.delete(userId, new DeleteHealthSamplesDto(List.of(uuid)));
        healthService.completeFullSync(userId, new CompleteFullSyncDto(HEART_RATE, UUID.randomUUID()));

        assertEquals(60, healthService.getSamples(otherUser, null, null, null, 0, 10).getFirst().value());
    }

    @Test
    void testSummaryAndRangeFilter() {
        healthService.upload(userId, new UploadHealthSamplesDto(null, List.of(
                heartRate(UUID.randomUUID(), 60, Instant.parse("2026-01-01T10:00:00Z")),
                heartRate(UUID.randomUUID(), 61, Instant.parse("2026-02-01T10:00:00Z")),
                steps(UUID.randomUUID()))));

        var summary = healthService.getSummary(userId);
        assertEquals(2, summary.size());
        assertEquals(HEART_RATE, summary.getFirst().type());
        assertEquals(2, summary.getFirst().count());

        var january = healthService.getSamples(userId, HEART_RATE, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-31T00:00:00Z"), 0, 10);
        assertEquals(1, january.size());
        assertEquals(60, january.getFirst().value());
    }

    @Test
    void testWriteRequestLifecycle() {
        HealthWriteRequestDto created = healthService.createWriteRequest(userId, new CreateHealthWriteRequestDto(
                HealthSampleKind.QUANTITY, "HKQuantityTypeIdentifierBodyMass", Instant.parse("2026-03-01T07:00:00Z"),
                Instant.parse("2026-03-01T07:00:00Z"), 80.5, "kg", null));
        assertEquals(HealthWriteRequestStatus.PENDING, created.status());
        assertEquals(1, healthService.getWriteRequests(userId, HealthWriteRequestStatus.PENDING).size());

        UUID written = UUID.randomUUID();
        HealthWriteRequestDto acked = healthService.acknowledgeWriteRequest(userId, created.id(),
                new AckHealthWriteRequestDto(written, null));
        assertEquals(HealthWriteRequestStatus.WRITTEN, acked.status());
        assertEquals(written, acked.sampleUuid());
        assertEquals(0, healthService.getWriteRequests(userId, HealthWriteRequestStatus.PENDING).size());
    }

    @Test
    void testWriteRequestWithoutUnitIsRejected() {
        assertThrows(ResponseStatusException.class, () -> healthService.createWriteRequest(userId,
                new CreateHealthWriteRequestDto(HealthSampleKind.QUANTITY, "HKQuantityTypeIdentifierBodyMass",
                        Instant.now(), Instant.now(), 80.5, null, null)));
    }

    private static HealthSampleDto heartRate(UUID uuid, double bpm) {
        return heartRate(uuid, bpm, Instant.parse("2026-01-01T10:00:00Z"));
    }

    private static HealthSampleDto heartRate(UUID uuid, double bpm, Instant date) {
        return new HealthSampleDto(uuid, HealthSampleKind.QUANTITY, HEART_RATE, date, date, bpm, "count/min",
                "Apple Watch", "com.apple.health", new JsonMapper().readTree("{\"device\":{\"name\":\"Apple Watch\"}}"));
    }

    private static HealthSampleDto steps(UUID uuid) {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        return new HealthSampleDto(uuid, HealthSampleKind.QUANTITY, STEPS, start, start.plusSeconds(600), 1200.0,
                "count", "iPhone", "com.apple.health", null);
    }
}

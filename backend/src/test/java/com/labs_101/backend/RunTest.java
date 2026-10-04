package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.health.HealthSampleDto;
import com.labs_101.backend.dtos.health.HealthSampleKind;
import com.labs_101.backend.dtos.health.UploadHealthSamplesDto;
import com.labs_101.backend.dtos.run.RunDetailDto;
import com.labs_101.backend.dtos.run.RunSummaryDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.HealthSampleRepository;
import com.labs_101.backend.repositories.HealthWriteRequestRepository;
import com.labs_101.backend.repositories.RunRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.HealthService;
import com.labs_101.backend.services.RunService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ HealthService.class, HealthSampleRepository.class, HealthWriteRequestRepository.class, RunService.class,
        RunRepository.class })
public class RunTest {
    private static final JsonMapper JSON = new JsonMapper();
    /** 09:00 in Berlin */
    private static final Instant START = Instant.parse("2026-09-20T07:00:00Z");
    private static final double SPEED = 10 / 3.0; // 5:00 /km
    private static final double DISTANCE = 5200;
    private static final double PAUSE_FROM = 600, PAUSE_TO = 660;

    @Autowired
    private HealthService healthService;
    @Autowired
    private RunService runService;
    @Autowired
    private UserRepository userRepository;

    private String userId;

    @BeforeEach
    void createUser() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
    }

    @Test
    void testRunSummary() {
        UUID id = UUID.randomUUID();
        upload(run(id, 37), cycling());

        List<RunSummaryDto> runs = runService.getRuns(userId, START.minusSeconds(86400), START.plusSeconds(86400));
        // the cycling workout is not a run
        assertEquals(1, runs.size());
        RunSummaryDto run = runs.getFirst();
        assertEquals(id, run.id());
        assertEquals("Morgenlauf", run.name());
        assertEquals(DISTANCE, run.distance(), 0.01);
        assertEquals(DISTANCE / SPEED, run.duration(), 0.01);
        assertEquals(12.34, run.elevationGain(), 0.001);
        assertEquals(150, run.averageHeartRate(), 0.01);
        assertEquals(170, run.maxHeartRate(), 0.01);
        assertFalse(run.indoor());
        assertTrue(run.thumbnail().size() > 10);

        // 140 bpm until 900 s are Z2, 165 bpm afterwards Z4
        assertEquals(900, run.zoneSeconds()[1], 10);
        assertEquals(DISTANCE / SPEED + PAUSE_TO - PAUSE_FROM - 900, run.zoneSeconds()[3], 10);

        assertEquals(1, run.bestEfforts().size());
        assertEquals("5 km", run.bestEfforts().getFirst().name());
        // the pause doesn't count
        assertEquals(1500, run.bestEfforts().getFirst().time(), 5);
    }

    @Test
    void testRunDetail() {
        UUID id = UUID.randomUUID();
        upload(run(id, 37));

        RunDetailDto detail = runService.getRun(userId, id);
        assertEquals(6, detail.splits().size());
        detail.splits().subList(0, 5).forEach(split -> {
            assertEquals(1000, split.distance(), 0.01);
            assertEquals(300, split.time(), 2);
        });
        assertEquals(200, detail.splits().getLast().distance(), 0.01);
        assertEquals(140, detail.splits().getFirst().heartRate(), 1);
        assertEquals(165, detail.splits().getLast().heartRate(), 1);

        var points = detail.points();
        assertEquals(DISTANCE, points.getLast().distance(), 0.01);
        assertEquals(DISTANCE / SPEED, points.getLast().time(), 2);
        // also right after the pause
        points.stream().skip(5).forEach(p -> assertEquals(300, p.pace(), 3));
    }

    @Test
    void testRunsOfOtherUsersAreNotFound() {
        UUID id = UUID.randomUUID();
        upload(run(id, 37));
        String otherUser = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();

        assertTrue(runService.getRuns(otherUser, null, null).isEmpty());
        assertThrows(NotFoundException.class, () -> runService.getRun(otherUser, id));
    }

    private void upload(HealthSampleDto... samples) {
        List<HealthSampleDto> all = new ArrayList<>(List.of(samples));
        // heart rate every 5 seconds as HealthKit stores it, in count/s
        double elapsed = DISTANCE / SPEED + PAUSE_TO - PAUSE_FROM;
        for (int t = 0; t < elapsed; t += 5) {
            Instant date = START.plusSeconds(t);
            all.add(new HealthSampleDto(UUID.randomUUID(), HealthSampleKind.QUANTITY,
                    "HKQuantityTypeIdentifierHeartRate", date, date, (t < 900 ? 140 : 165) / 60.0, "count/s",
                    "Apple Watch", "com.apple.health", null));
        }
        healthService.upload(userId, new UploadHealthSamplesDto(null, all));
    }

    /** Straight north at 5:00 /km, standing still during the pause. */
    private static HealthSampleDto run(UUID id, int activityType) {
        double movingTime = DISTANCE / SPEED;
        double elapsed = movingTime + PAUSE_TO - PAUSE_FROM;
        double metersPerDegree = 6371000 * Math.PI / 180;

        ArrayNode points = JSON.createArrayNode();
        for (int t = 0; t <= elapsed; t++) {
            double moving = t < PAUSE_FROM ? t : t < PAUSE_TO ? PAUSE_FROM : t - (PAUSE_TO - PAUSE_FROM);
            points.addArray().add(t).add(52.47 + moving * SPEED / metersPerDegree).add(13.40).add(40.0)
                    .add(SPEED).add(0).add(5).add(3);
        }

        ObjectNode payload = JSON.createObjectNode();
        payload.put("activityType", activityType);
        payload.putObject("metadata").put("HKElevationAscended", "1234 cm").put("HKTimeZone", "Europe/Berlin");
        ObjectNode statistics = payload.putObject("statistics");
        statistics.putObject("HKQuantityTypeIdentifierDistanceWalkingRunning").put("unit", "m").put("sum", DISTANCE);
        statistics.putObject("HKQuantityTypeIdentifierHeartRate").put("unit", "count/s").put("average", 2.5)
                .put("maximum", 170 / 60.0);
        ArrayNode events = payload.putArray("events");
        events.addObject().put("type", 1).put("startDate", START.plusSeconds((long) PAUSE_FROM).toString());
        events.addObject().put("type", 2).put("startDate", START.plusSeconds((long) PAUSE_TO).toString());
        payload.putArray("routes").addObject().put("uuid", UUID.randomUUID().toString()).set("points", points);

        return workout(id, START.plusSeconds((long) elapsed), movingTime, payload);
    }

    private static HealthSampleDto cycling() {
        ObjectNode payload = JSON.createObjectNode().put("activityType", 13);
        return workout(UUID.randomUUID(), START.plusSeconds(7200), 3600, payload);
    }

    private static HealthSampleDto workout(UUID id, Instant end, double duration, JsonNode payload) {
        return new HealthSampleDto(id, HealthSampleKind.WORKOUT, "HKWorkoutTypeIdentifier", START, end, duration, "s",
                "Apple Watch", "com.apple.health", payload);
    }
}

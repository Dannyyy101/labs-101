package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.labs_101.backend.dtos.overview.HealthOverviewDto;
import com.labs_101.backend.dtos.overview.SleepDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.HealthOverviewRepository;
import com.labs_101.backend.repositories.HealthSampleRepository;
import com.labs_101.backend.repositories.HealthWriteRequestRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.HealthOverviewService;
import com.labs_101.backend.services.HealthService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ HealthService.class, HealthSampleRepository.class, HealthWriteRequestRepository.class,
        HealthOverviewService.class, HealthOverviewRepository.class })
public class HealthOverviewTest {
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final LocalDate DAY = LocalDate.of(2026, 9, 20);
    private static final String WATCH = "com.apple.health.watch";
    private static final String IPHONE = "com.apple.health.iphone";

    @Autowired
    private HealthService healthService;
    @Autowired
    private HealthOverviewService overviewService;
    @Autowired
    private UserRepository userRepository;

    private String userId;
    private final List<HealthSampleDto> samples = new ArrayList<>();

    @BeforeEach
    void createUser() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
    }

    @Test
    void testSleepStages() {
        // the iPhone's time in bed is ignored, the Watch recorded the stages
        sleep(IPHONE, 0, at(DAY.minusDays(1), 21, 30), at(DAY, 6, 30));
        sleep(WATCH, 3, at(DAY.minusDays(1), 22, 0), at(DAY, 1, 0));
        sleep(WATCH, 4, at(DAY, 1, 0), at(DAY, 2, 0));
        sleep(WATCH, 2, at(DAY, 2, 0), at(DAY, 2, 30));
        sleep(WATCH, 5, at(DAY, 2, 30), at(DAY, 4, 0));
        sleep(WATCH, 3, at(DAY, 4, 0), at(DAY, 6, 0));
        // awake after waking up isn't part of the sleep
        sleep(WATCH, 2, at(DAY, 6, 0), at(DAY, 6, 20));
        upload();

        SleepDto sleep = overview().sleep();
        assertNotNull(sleep);
        assertEquals(at(DAY.minusDays(1), 22, 0), sleep.start());
        assertEquals(at(DAY, 6, 0), sleep.end());
        assertEquals(7.5 * 3600, sleep.asleep(), 1);
        assertEquals(8 * 3600, sleep.inBed(), 1);
        assertEquals(0.5 * 3600, sleep.awake(), 1);
        assertEquals(3600, sleep.deep(), 1);
        assertEquals(1.5 * 3600, sleep.rem(), 1);
        assertEquals(5 * 3600, sleep.core(), 1);
        assertEquals(5, sleep.stages().size());
        assertTrue(sleep.score() > 70 && sleep.score() < 100, "score " + sleep.score());
    }

    @Test
    void testStepsOfSeveralDevicesAreNotCountedTwice() {
        quantity(WATCH, "HKQuantityTypeIdentifierStepCount", at(DAY, 10, 0), at(DAY, 10, 30), 1000, "count");
        quantity(IPHONE, "HKQuantityTypeIdentifierStepCount", at(DAY, 10, 10), at(DAY, 10, 40), 800, "count");
        quantity(IPHONE, "HKQuantityTypeIdentifierStepCount", at(DAY, 14, 0), at(DAY, 14, 10), 500, "count");
        // the day before
        quantity(IPHONE, "HKQuantityTypeIdentifierStepCount", at(DAY.minusDays(1), 23, 0), at(DAY.minusDays(1), 23, 10), 300, "count");
        for (int hour = 8; hour < 12; hour++)
            category("HKCategoryTypeIdentifierAppleStandHour", at(DAY, hour, 0), at(DAY, hour + 1, 0), hour == 11 ? 1 : 0);
        upload();

        HealthOverviewDto overview = overview();
        assertEquals(1500, overview.activity().steps(), 0.01);
        assertEquals(3, overview.activity().standHours());
        assertEquals(300, overview.history().get(overview.history().size() - 2).steps(), 0.01);
    }

    @Test
    void testRecoveryNeedsABaseline() {
        night(DAY, 50, 60);
        upload();
        HealthOverviewDto overview = overview();
        assertEquals(50, overview.recovery().hrv(), 0.01);
        assertNull(overview.recovery().score());
    }

    @Test
    void testRecoveryComparesWithTheDaysBefore() {
        double[] hrv = { 45, 55, 50, 48, 52 };
        for (int i = 0; i < hrv.length; i++)
            night(DAY.minusDays(i + 1), hrv[i], 60);
        night(DAY, 50, 60);
        upload();

        HealthOverviewDto overview = overview();
        assertEquals(50, overview.recovery().hrvBaseline(), 0.01);
        assertEquals(60, overview.recovery().restingHeartRate(), 0.01);
        // an average night scores 60, blended with the sleep
        assertEquals(Math.round(0.75 * 60 + 0.25 * overview.sleep().score()), overview.recovery().score(), 2);
    }

    @Test
    void testHighHrvMeansBetterRecovery() {
        for (int i = 1; i <= 5; i++)
            night(DAY.minusDays(i), 48 + i, 60);
        night(DAY, 80, 55);
        upload();
        assertTrue(overview().recovery().score() > 80);
    }

    @Test
    void testStrain() {
        // an hour at 140 bpm, maximum heart rate 190 without a date of birth or workouts
        for (int t = 0; t < 3600; t += 5) {
            Instant date = at(DAY, 17, 0).plusSeconds(t);
            quantity(WATCH, "HKQuantityTypeIdentifierHeartRate", date, date, 140 / 60.0, "count/s");
        }
        upload();

        HealthOverviewDto overview = overview();
        assertEquals(190, overview.strain().maxHeartRate(), 0.01);
        // 140 / 190 = 74 % is Z3
        // the last sample counts a minute
        assertEquals(3660, overview.strain().zoneSeconds()[2], 10);
        assertTrue(overview.strain().score() > 25 && overview.strain().score() < 45, "strain " + overview.strain().score());
        assertEquals(overview.strain().score(), overview.history().getLast().strain());
        assertEquals(140, overview.strain().averageHeartRate(), 0.01);
        assertEquals(12, overview.heartRate().size());
    }

    @Test
    void testEmptyDay() {
        HealthOverviewDto overview = overview();
        assertNull(overview.sleep());
        assertNull(overview.recovery().score());
        assertEquals(0, overview.strain().score());
        assertEquals(14, overview.history().size());
        assertEquals(DAY, overview.history().getLast().date());
    }

    private HealthOverviewDto overview() {
        return overviewService.getOverview(userId, DAY, BERLIN);
    }

    /** A night of core sleep ending on the day with the HRV measured during it. */
    private void night(LocalDate day, double hrv, double restingHeartRate) {
        sleep(WATCH, 3, at(day.minusDays(1), 23, 0), at(day, 7, 0));
        quantity(WATCH, "HKQuantityTypeIdentifierHeartRateVariabilitySDNN", at(day, 3, 0), at(day, 3, 1), hrv, "ms");
        quantity(WATCH, "HKQuantityTypeIdentifierRestingHeartRate", at(day, 8, 0), at(day, 20, 0), restingHeartRate, "count/min");
    }

    private void sleep(String source, int value, Instant start, Instant end) {
        samples.add(new HealthSampleDto(UUID.randomUUID(), HealthSampleKind.CATEGORY,
                "HKCategoryTypeIdentifierSleepAnalysis", start, end, (double) value, null, source, source, null));
    }

    private void category(String type, Instant start, Instant end, int value) {
        samples.add(new HealthSampleDto(UUID.randomUUID(), HealthSampleKind.CATEGORY, type, start, end,
                (double) value, null, WATCH, WATCH, null));
    }

    private void quantity(String source, String type, Instant start, Instant end, double value, String unit) {
        samples.add(new HealthSampleDto(UUID.randomUUID(), HealthSampleKind.QUANTITY, type, start, end, value, unit,
                source, source, null));
    }

    private void upload() {
        healthService.upload(userId, new UploadHealthSamplesDto(null, samples));
    }

    private static Instant at(LocalDate day, int hour, int minute) {
        return LocalDateTime.of(day, java.time.LocalTime.of(hour, minute)).atZone(BERLIN).toInstant();
    }
}

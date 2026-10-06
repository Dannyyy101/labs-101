package com.labs_101.backend.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.overview.ActivityDto;
import com.labs_101.backend.dtos.overview.DayScoreDto;
import com.labs_101.backend.dtos.overview.HealthOverviewDto;
import com.labs_101.backend.dtos.overview.OverviewWorkoutDto;
import com.labs_101.backend.dtos.overview.RecoveryDto;
import com.labs_101.backend.dtos.overview.SleepDto;
import com.labs_101.backend.dtos.overview.SleepStageDto;
import com.labs_101.backend.dtos.overview.StrainDto;
import com.labs_101.backend.dtos.overview.VitalsDto;
import com.labs_101.backend.repositories.HealthOverviewRepository;
import com.labs_101.backend.repositories.HealthOverviewRepository.HeartRateDayRow;
import com.labs_101.backend.repositories.HealthOverviewRepository.SampleRow;
import com.labs_101.backend.repositories.HealthOverviewRepository.WorkoutRow;

import tools.jackson.databind.JsonNode;

/**
 * The home page: sleep, recovery and strain of a day, in the spirit of apps
 * like Bevel or Whoop, computed from the Apple Health data the app syncs.
 *
 * <ul>
 * <li><b>Sleep</b> from the Watch's sleep stages, scored on duration,
 * efficiency and the share of deep and REM sleep.</li>
 * <li><b>Recovery</b> compares the HRV during sleep and the resting heart
 * rate with the 30 days before, and includes the sleep score.</li>
 * <li><b>Strain</b> is the Banister TRIMP of the day's heart rate, mapped to
 * 0–100 so every point gets harder to reach.</li>
 * </ul>
 */
@Service
public class HealthOverviewService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");
    /** Days in the history of the scores. */
    static final int HISTORY_DAYS = 14;
    /** Days before a day its HRV and resting heart rate are compared with. */
    static final int BASELINE_DAYS = 30;
    /** Days with HRV the baseline needs before there is a recovery score. */
    static final int MIN_BASELINE_DAYS = 3;
    static final double SLEEP_NEED = 8 * 3600;
    /** Samples further apart than this belong to different sleeps. */
    private static final long SLEEP_GAP = 3600;
    /** Load at which the strain is 63 %, 86 % at twice the load. */
    static final double STRAIN_SCALE = 180;
    /** Only heart rate above this fraction of the reserve counts for the strain, sitting around isn't strain. */
    static final double MIN_RESERVE = 0.3;
    /** Lower bounds of Z1…Z5 as fractions of the maximum heart rate. */
    static final double[] ZONE_BOUNDS = { 0.5, 0.6, 0.7, 0.8, 0.9 };

    private static final String SLEEP = "HKCategoryTypeIdentifierSleepAnalysis";
    private static final String HRV = "HKQuantityTypeIdentifierHeartRateVariabilitySDNN";
    private static final String RESTING_HEART_RATE = "HKQuantityTypeIdentifierRestingHeartRate";
    private static final String RESPIRATORY_RATE = "HKQuantityTypeIdentifierRespiratoryRate";
    private static final String OXYGEN_SATURATION = "HKQuantityTypeIdentifierOxygenSaturation";
    private static final String WRIST_TEMPERATURE = "HKQuantityTypeIdentifierAppleSleepingWristTemperature";
    private static final String VO2_MAX = "HKQuantityTypeIdentifierVO2Max";
    private static final String BODY_MASS = "HKQuantityTypeIdentifierBodyMass";

    private static final String STEPS = "HKQuantityTypeIdentifierStepCount";
    private static final String DISTANCE = "HKQuantityTypeIdentifierDistanceWalkingRunning";
    private static final String ACTIVE_ENERGY = "HKQuantityTypeIdentifierActiveEnergyBurned";
    private static final String BASAL_ENERGY = "HKQuantityTypeIdentifierBasalEnergyBurned";
    private static final String EXERCISE_TIME = "HKQuantityTypeIdentifierAppleExerciseTime";
    private static final String FLIGHTS = "HKQuantityTypeIdentifierFlightsClimbed";
    private static final String DAYLIGHT = "HKQuantityTypeIdentifierTimeInDaylight";
    private static final String ENERGY_CONSUMED = "HKQuantityTypeIdentifierDietaryEnergyConsumed";
    private static final List<String> DAILY_SUMS = List.of(STEPS, DISTANCE, ACTIVE_ENERGY, BASAL_ENERGY,
            EXERCISE_TIME, FLIGHTS, DAYLIGHT, ENERGY_CONSUMED, HealthOverviewRepository.STAND_HOUR);

    // HKCategoryValueSleepAnalysis
    private static final int ASLEEP = 1, AWAKE = 2, CORE = 3, DEEP = 4, REM = 5;

    private final HealthOverviewRepository repository;

    HealthOverviewService(HealthOverviewRepository repository) {
        this.repository = repository;
    }

    public HealthOverviewDto getOverview(String userId, LocalDate date, ZoneId zone) {
        LocalDate first = date.minusDays(HISTORY_DAYS - 1);
        LocalDate firstBaseline = first.minusDays(BASELINE_DAYS);
        // a sleep ending on a day starts the evening before
        Instant from = firstBaseline.minusDays(1).atStartOfDay(zone).toInstant();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<SampleRow> samples = repository.samples(userId,
                List.of(SLEEP, HRV, RESTING_HEART_RATE, RESPIRATORY_RATE, OXYGEN_SATURATION, WRIST_TEMPERATURE),
                from, to);
        Map<String, List<SampleRow>> byType = new HashMap<>();
        samples.forEach(s -> byType.computeIfAbsent(s.type(), t -> new ArrayList<>()).add(s));

        Map<LocalDate, List<Sleep>> sleeps = sleeps(byType.getOrDefault(SLEEP, List.of()), zone);
        Map<LocalDate, Night> nights = new HashMap<>();
        for (LocalDate d = firstBaseline; !d.isAfter(date); d = d.plusDays(1))
            nights.put(d, night(d, sleeps.getOrDefault(d, List.of()), byType, zone));

        // strain: the resting heart rate of the baseline, the maximum from age and workouts
        Double restingBaseline = mean(nights, date, Night::restingHeartRate);
        double resting = restingBaseline != null ? restingBaseline : 60;
        double maxHeartRate = maxHeartRate(userId, date, zone);
        Map<LocalDate, HeartRateDayRow> heartRate = repository.heartRateDays(userId, zone,
                first.atStartOfDay(zone).toInstant(), to, resting, maxHeartRate, MIN_RESERVE, ZONE_BOUNDS);
        Map<LocalDate, Map<String, Double>> sums = repository.dailySums(userId, DAILY_SUMS, zone,
                first.atStartOfDay(zone).toInstant(), to);

        List<DayScoreDto> history = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(date); d = d.plusDays(1)) {
            Night night = nights.get(d);
            Map<String, Double> day = sums.getOrDefault(d, Map.of());
            HeartRateDayRow hr = heartRate.get(d);
            history.add(new DayScoreDto(d, recoveryScore(nights, d), hr != null ? strainScore(hr.load()) : null,
                    night.sleep() != null ? night.sleep().score() : null,
                    night.sleep() != null ? night.sleep().asleep() : null,
                    night.hrv(), night.restingHeartRate(),
                    day.getOrDefault(STEPS, 0.0), day.getOrDefault(ACTIVE_ENERGY, 0.0)));
        }

        Night night = nights.get(date);
        Map<String, Double> day = sums.getOrDefault(date, Map.of());
        HeartRateDayRow hr = heartRate.get(date);
        Double wristBaseline = mean(nights, date, Night::wristTemperature);

        return new HealthOverviewDto(
                date,
                zone.getId(),
                night.sleep(),
                new RecoveryDto(
                        recoveryScore(nights, date),
                        night.hrv(), mean(nights, date, Night::hrv),
                        night.restingHeartRate(), restingBaseline,
                        night.respiratoryRate(), mean(nights, date, Night::respiratoryRate),
                        night.oxygenSaturation(),
                        night.wristTemperature() != null && wristBaseline != null
                                ? night.wristTemperature() - wristBaseline
                                : null),
                hr != null
                        ? new StrainDto(strainScore(hr.load()), hr.load(), zones(hr.zoneSeconds()), maxHeartRate,
                                hr.seconds() > 0 ? hr.weightedBpm() / hr.seconds() : null, hr.maxBpm())
                        : new StrainDto(0, 0, new double[ZONE_BOUNDS.length], maxHeartRate, null, null),
                new ActivityDto(
                        day.getOrDefault(STEPS, 0.0),
                        day.getOrDefault(DISTANCE, 0.0),
                        day.getOrDefault(ACTIVE_ENERGY, 0.0),
                        day.getOrDefault(BASAL_ENERGY, 0.0),
                        day.getOrDefault(EXERCISE_TIME, 0.0),
                        (int) Math.round(day.getOrDefault(HealthOverviewRepository.STAND_HOUR, 0.0)),
                        day.getOrDefault(FLIGHTS, 0.0),
                        day.getOrDefault(DAYLIGHT, 0.0),
                        day.get(ENERGY_CONSUMED)),
                repository.heartRateSeries(userId, dayStart, to, 300),
                repository.workouts(userId, dayStart, to).stream().map(HealthOverviewService::workout).toList(),
                vitals(userId, to),
                history);
    }

    // MARK: sleep

    /** One sleep, the stages are from the device that recorded the most of it. */
    record Sleep(Instant start, Instant end, double asleep, double deep, double core, double rem,
            List<SleepStageDto> stages) {
    }

    /** Sleeps by the local day they end on. */
    static Map<LocalDate, List<Sleep>> sleeps(List<SampleRow> samples, ZoneId zone) {
        Map<LocalDate, List<Sleep>> sleeps = new HashMap<>();
        List<SampleRow> group = new ArrayList<>();
        Instant groupEnd = null;
        for (SampleRow s : samples.stream().sorted(Comparator.comparing(SampleRow::startDate)).toList()) {
            if (groupEnd != null && s.startDate().getEpochSecond() - groupEnd.getEpochSecond() > SLEEP_GAP) {
                addSleep(sleeps, group, zone);
                group = new ArrayList<>();
                groupEnd = null;
            }
            group.add(s);
            if (groupEnd == null || s.endDate().isAfter(groupEnd))
                groupEnd = s.endDate();
        }
        addSleep(sleeps, group, zone);
        return sleeps;
    }

    private static void addSleep(Map<LocalDate, List<Sleep>> sleeps, List<SampleRow> group, ZoneId zone) {
        // several devices or apps can record the same night, the one with the most sleep wins
        Map<String, Double> asleepBySource = new HashMap<>();
        for (SampleRow s : group)
            if (isAsleep(s))
                asleepBySource.merge(String.valueOf(s.source()), seconds(s), Double::sum);
        String source = asleepBySource.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (source == null)
            return;

        List<SampleRow> rows = group.stream()
                .filter(s -> source.equals(String.valueOf(s.source())) && (isAsleep(s) || (int) s.value() == AWAKE))
                .toList();
        double asleep = 0, deep = 0, core = 0, rem = 0;
        List<SleepStageDto> stages = new ArrayList<>();
        for (SampleRow s : rows) {
            double seconds = seconds(s);
            switch ((int) s.value()) {
                case DEEP -> deep += seconds;
                case CORE -> core += seconds;
                case REM -> rem += seconds;
                default -> {
                }
            }
            if (isAsleep(s))
                asleep += seconds;
            stages.add(new SleepStageDto(stage((int) s.value()), s.startDate(), s.endDate()));
        }
        // awake before falling asleep and after waking up isn't part of the sleep
        Instant start = rows.stream().filter(HealthOverviewService::isAsleep).map(SampleRow::startDate)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant end = rows.stream().filter(HealthOverviewService::isAsleep).map(SampleRow::endDate)
                .max(Comparator.naturalOrder()).orElseThrow();
        stages.removeIf(s -> !s.end().isAfter(start) || !s.start().isBefore(end));

        sleeps.computeIfAbsent(LocalDate.ofInstant(end, zone), d -> new ArrayList<>())
                .add(new Sleep(start, end, asleep, deep, core, rem, stages));
    }

    private static boolean isAsleep(SampleRow s) {
        int value = (int) s.value();
        return value == ASLEEP || value == CORE || value == DEEP || value == REM;
    }

    private static String stage(int value) {
        return switch (value) {
            case AWAKE -> "AWAKE";
            case CORE -> "CORE";
            case DEEP -> "DEEP";
            case REM -> "REM";
            default -> "ASLEEP";
        };
    }

    /**
     * 0–100: 50 points for sleeping {@link #SLEEP_NEED}, 20 for an efficiency
     * of 95 % (80 % or less gives none) and 15 each for 15 % deep and 22 % REM
     * sleep. Without stages those count half.
     */
    static int sleepScore(double asleep, double inBed, double deep, double rem) {
        double duration = 50 * Math.min(1, asleep / SLEEP_NEED);
        double efficiency = inBed > 0 ? 20 * Math.clamp((asleep / inBed - 0.8) / 0.15, 0, 1) : 0;
        double stages = deep + rem > 0 && asleep > 0
                ? 15 * Math.min(1, deep / asleep / 0.15) + 15 * Math.min(1, rem / asleep / 0.22)
                : 15;
        return (int) Math.round(duration + efficiency + stages);
    }

    // MARK: recovery

    /** What the body measured during the main sleep ending on a day. */
    record Night(SleepDto sleep, Double hrv, Double restingHeartRate, Double respiratoryRate,
            Double oxygenSaturation, Double wristTemperature) {
    }

    private static Night night(LocalDate date, List<Sleep> sleeps, Map<String, List<SampleRow>> samples, ZoneId zone) {
        Double restingHeartRate = average(samples.get(RESTING_HEART_RATE),
                s -> LocalDate.ofInstant(s.startDate(), zone).equals(date));
        Sleep main = sleeps.stream().max(Comparator.comparingDouble(Sleep::asleep)).orElse(null);
        if (main == null)
            return new Night(null, null, restingHeartRate, null, null, null);

        // HealthKit records HRV every few hours, give it some time around the sleep
        Instant from = main.start().minusSeconds(900), to = main.end().plusSeconds(900);
        Predicate<SampleRow> during = s -> !s.startDate().isBefore(from) && s.startDate().isBefore(to);
        Double hrv = average(samples.get(HRV), during);
        Double respiratoryRate = average(samples.get(RESPIRATORY_RATE), during);
        if (respiratoryRate != null && "count/s".equals(samples.get(RESPIRATORY_RATE).getFirst().unit()))
            respiratoryRate *= 60;
        Double oxygen = average(samples.get(OXYGEN_SATURATION), during);
        if (oxygen != null && oxygen > 1)
            oxygen /= 100;
        Double wrist = average(samples.get(WRIST_TEMPERATURE), s -> s.endDate().isAfter(main.start())
                && LocalDate.ofInstant(s.endDate(), zone).equals(date));

        // naps count for the duration, as if they were spent asleep in bed
        double asleep = sleeps.stream().mapToDouble(Sleep::asleep).sum();
        double inBed = (main.end().toEpochMilli() - main.start().toEpochMilli()) / 1000.0;
        SleepDto sleep = new SleepDto(main.start(), main.end(), asleep, inBed, Math.max(0, inBed - main.asleep()),
                main.deep(), main.core(), main.rem(), SLEEP_NEED,
                sleepScore(asleep, inBed + asleep - main.asleep(), main.deep(), main.rem()),
                respiratoryRate, oxygen, main.stages());
        return new Night(sleep, hrv, restingHeartRate, respiratoryRate, oxygen, wrist);
    }

    /**
     * 0–100: HRV (on a log scale, it's skewed) and resting heart rate as
     * standard scores against the 30 days before, 60 on an average night,
     * blended with the sleep score. Null without HRV or enough history.
     */
    static Integer recoveryScore(Map<LocalDate, Night> nights, LocalDate date) {
        Night night = nights.get(date);
        if (night == null || night.hrv() == null)
            return null;
        double[] hrv = baseline(nights, date, n -> n.hrv() != null ? Math.log(n.hrv()) : null);
        if (hrv == null)
            return null;
        double z = (Math.log(night.hrv()) - hrv[0]) / Math.max(hrv[1], 0.1);
        double[] resting = baseline(nights, date, Night::restingHeartRate);
        double physiology = 0.65 * Math.clamp(z, -2.5, 2.5);
        if (resting != null && night.restingHeartRate() != null)
            physiology -= 0.35 * Math.clamp((night.restingHeartRate() - resting[0]) / Math.max(resting[1], 2), -2.5, 2.5);
        else
            physiology /= 0.65;
        double score = 60 + 25 * physiology;
        if (night.sleep() != null)
            score = 0.75 * score + 0.25 * night.sleep().score();
        return (int) Math.round(Math.clamp(score, 1, 99));
    }

    /** [mean, standard deviation] of the 30 days before, null with less than 3 values. */
    private static double[] baseline(Map<LocalDate, Night> nights, LocalDate date, Function<Night, Double> value) {
        List<Double> values = new ArrayList<>();
        for (int i = 1; i <= BASELINE_DAYS; i++) {
            Night n = nights.get(date.minusDays(i));
            Double v = n != null ? value.apply(n) : null;
            if (v != null)
                values.add(v);
        }
        if (values.size() < MIN_BASELINE_DAYS)
            return null;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / (values.size() - 1);
        return new double[] { mean, Math.sqrt(variance) };
    }

    private static Double mean(Map<LocalDate, Night> nights, LocalDate date, Function<Night, Double> value) {
        double[] baseline = baseline(nights, date, value);
        return baseline != null ? baseline[0] : null;
    }

    // MARK: strain

    static int strainScore(double load) {
        return (int) Math.round(100 * (1 - Math.exp(-load / STRAIN_SCALE)));
    }

    /** Tanaka's formula for the age, or the highest heart rate of a workout in the last year if that's higher. */
    private double maxHeartRate(String userId, LocalDate date, ZoneId zone) {
        double max = repository.dateOfBirth(userId)
                .map(birth -> 208 - 0.7 * Period.between(birth, date).getYears())
                .orElse(190.0);
        Double workouts = repository.maxWorkoutHeartRate(userId,
                date.minusYears(1).atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
        return workouts != null ? Math.max(max, workouts) : max;
    }

    /** Z1–Z5, the time below Z1 is left out. */
    private static double[] zones(double[] buckets) {
        double[] zones = new double[ZONE_BOUNDS.length];
        System.arraycopy(buckets, 1, zones, 0, zones.length);
        return zones;
    }

    // MARK: rest

    private static OverviewWorkoutDto workout(WorkoutRow row) {
        double duration = row.duration() != null ? row.duration()
                : (row.endDate().toEpochMilli() - row.startDate().toEpochMilli()) / 1000.0;
        Double distance = null;
        if (row.statistics() != null) {
            for (var entry : row.statistics().properties()) {
                if (entry.getKey().startsWith("HKQuantityTypeIdentifierDistance") && entry.getValue().has("sum")) {
                    double sum = entry.getValue().get("sum").asDouble();
                    distance = (distance != null ? distance : 0)
                            + ("km".equals(entry.getValue().path("unit").asString()) ? sum * 1000 : sum);
                }
            }
        }
        JsonNode heartRate = row.statistics() != null ? row.statistics().get(HealthOverviewRepository.HEART_RATE) : null;
        Double averageHeartRate = heartRate != null && heartRate.has("average")
                ? heartRate.get("average").asDouble() * ("count/s".equals(heartRate.path("unit").asString()) ? 60 : 1)
                : null;
        JsonNode energy = row.statistics() != null ? row.statistics().get(ACTIVE_ENERGY) : null;
        return new OverviewWorkoutDto(row.uuid(), row.activityType(), row.startDate(), row.endDate(), duration,
                energy != null && energy.has("sum") ? energy.get("sum").asDouble() : null,
                distance, averageHeartRate);
    }

    private VitalsDto vitals(String userId, Instant before) {
        Double bodyMass = repository.latest(userId, BODY_MASS, before).orElse(null);
        Double previous = repository.latest(userId, BODY_MASS, before.minusSeconds(30 * 86400L)).orElse(null);
        return new VitalsDto(repository.latest(userId, VO2_MAX, before).orElse(null), bodyMass,
                bodyMass != null && previous != null ? bodyMass - previous : null);
    }

    private static double seconds(SampleRow s) {
        return (s.endDate().toEpochMilli() - s.startDate().toEpochMilli()) / 1000.0;
    }

    private static Double average(List<SampleRow> samples, Predicate<SampleRow> filter) {
        if (samples == null)
            return null;
        var stats = samples.stream().filter(filter).mapToDouble(SampleRow::value).summaryStatistics();
        return stats.getCount() > 0 ? stats.getAverage() : null;
    }
}

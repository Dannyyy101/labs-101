package com.labs_101.backend.repositories;

import static com.labs_101.backend.repositories.HealthSampleRepository.getInstant;
import static com.labs_101.backend.repositories.HealthSampleRepository.parseJson;
import static com.labs_101.backend.repositories.HealthSampleRepository.toOffsetDateTime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.JsonNode;

/**
 * Daily figures for the home page out of the synced Apple Health data (see
 * {@link HealthSampleRepository}). Heart rate and the cumulative types have
 * far too many samples to load them, they are aggregated in the database.
 */
@Repository
public class HealthOverviewRepository {

    public static final String HEART_RATE = "HKQuantityTypeIdentifierHeartRate";
    public static final String STAND_HOUR = "HKCategoryTypeIdentifierAppleStandHour";
    private static final String WORKOUT = "HKWorkoutTypeIdentifier";

    /** Heart rate in bpm, HealthKit stores it as count/s. */
    private static final String BPM = "CASE WHEN unit = 'count/s' THEN value * 60 ELSE value END";

    public record SampleRow(String type, Instant startDate, Instant endDate, double value, String unit,
            String source) {
    }

    /**
     * @param load        Banister TRIMP of the time above {@code minReserve} of the heart rate reserve
     * @param zoneSeconds index 0 is below the first bound
     */
    public record HeartRateDayRow(double load, double[] zoneSeconds, double seconds, double weightedBpm,
            double maxBpm) {
    }

    public record WorkoutRow(UUID uuid, int activityType, Instant startDate, Instant endDate, Double duration,
            JsonNode statistics) {
    }

    private final JdbcTemplate jdbc;

    public HealthOverviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Raw samples of the types starting in the range, oldest first. Only for types with few samples. */
    public List<SampleRow> samples(String userId, List<String> types, Instant from, Instant to) {
        List<Object> args = new ArrayList<>(List.of(userId));
        args.addAll(types);
        args.add(toOffsetDateTime(from));
        args.add(toOffsetDateTime(to));
        return jdbc.query("""
                SELECT type, start_date, end_date, value, unit, source_bundle_id
                FROM health_sample
                WHERE user_id = ? AND type IN (%s) AND start_date >= ? AND start_date < ? AND value IS NOT NULL
                ORDER BY start_date
                """.formatted(placeholders(types.size())),
                (rs, i) -> new SampleRow(rs.getString("type"), getInstant(rs, "start_date"),
                        getInstant(rs, "end_date"), rs.getDouble("value"), rs.getString("unit"),
                        rs.getString("source_bundle_id")),
                args.toArray());
    }

    /**
     * Sum per local day and type. iPhone and Watch both count steps, so per
     * hour only the device with the larger sum counts, like Apple Health
     * does it roughly. A stand hour counts 1 when the user stood.
     */
    public Map<LocalDate, Map<String, Double>> dailySums(String userId, List<String> types, ZoneId zone,
            Instant from, Instant to) {
        List<Object> args = new ArrayList<>(List.of(STAND_HOUR, zone.getId(), userId));
        args.addAll(types);
        args.add(toOffsetDateTime(from));
        args.add(toOffsetDateTime(to));
        Map<LocalDate, Map<String, Double>> days = new HashMap<>();
        jdbc.query("""
                SELECT type, hour::date AS day, sum(value) AS value
                FROM (
                    SELECT type, hour, max(value) AS value
                    FROM (
                        SELECT type, source_bundle_id,
                            sum(CASE WHEN type = ? THEN 1 - value ELSE value END) AS value,
                            date_trunc('hour', start_date AT TIME ZONE ?) AS hour
                        FROM health_sample
                        WHERE user_id = ? AND type IN (%s) AND start_date >= ? AND start_date < ? AND value IS NOT NULL
                        GROUP BY type, source_bundle_id, hour
                    ) per_source
                    GROUP BY type, hour
                ) per_hour
                GROUP BY type, day
                """.formatted(placeholders(types.size())), (RowCallbackHandler) rs -> days
                .computeIfAbsent(rs.getObject("day", LocalDate.class), d -> new HashMap<>())
                .put(rs.getString("type"), rs.getDouble("value")), args.toArray());
        return days;
    }

    /**
     * Heart rate per local day. A sample counts until the next one, at most
     * 10 minutes, so the Watch's sparse readings during the day and the
     * dense ones during workouts are weighted by the time they stand for.
     *
     * @param zoneBounds lower bounds of Z1…Z5 as fractions of {@code maxBpm}
     */
    public Map<LocalDate, HeartRateDayRow> heartRateDays(String userId, ZoneId zone, Instant from, Instant to,
            double restingBpm, double maxBpm, double minReserve, double[] zoneBounds) {
        StringBuilder array = new StringBuilder();
        for (double bound : zoneBounds)
            array.append(array.isEmpty() ? "" : ",").append(bound * maxBpm);
        Map<LocalDate, HeartRateDayRow> days = new HashMap<>();
        jdbc.query("""
                WITH hr AS (
                    SELECT start_date, %s AS bpm,
                        least(coalesce(extract(EPOCH FROM lead(start_date) OVER (ORDER BY start_date) - start_date), 60), 600) AS seconds
                    FROM health_sample
                    WHERE user_id = ? AND type = ? AND start_date >= ? AND start_date < ? AND value IS NOT NULL
                ),
                reserve AS (
                    SELECT (start_date AT TIME ZONE ?)::date AS day, bpm, seconds,
                        greatest(0, least(1, (bpm - ?) / (? - ?))) AS r
                    FROM hr
                )
                SELECT day, width_bucket(bpm, ARRAY[%s]::float8[]) AS zone,
                    coalesce(sum(seconds / 60 * r * 0.64 * exp(1.92 * r)) FILTER (WHERE r > ?), 0) AS load,
                    sum(seconds) AS seconds, sum(seconds * bpm) AS weighted_bpm, max(bpm) AS max_bpm
                FROM reserve
                GROUP BY day, zone
                """.formatted(BPM, array), (RowCallbackHandler) rs -> {
            LocalDate day = rs.getObject("day", LocalDate.class);
            HeartRateDayRow row = days.getOrDefault(day, new HeartRateDayRow(0, new double[zoneBounds.length + 1], 0, 0, 0));
            double[] zones = row.zoneSeconds();
            zones[rs.getInt("zone")] += rs.getDouble("seconds");
            days.put(day, new HeartRateDayRow(row.load() + rs.getDouble("load"), zones,
                    row.seconds() + rs.getDouble("seconds"), row.weightedBpm() + rs.getDouble("weighted_bpm"),
                    Math.max(row.maxBpm(), rs.getDouble("max_bpm"))));
        }, userId, HEART_RATE, toOffsetDateTime(from), toOffsetDateTime(to), zone.getId(), restingBpm, maxBpm,
                restingBpm, minReserve);
        return days;
    }

    /** Average heart rate per {@code seconds} as [epoch millis, bpm]. */
    public List<double[]> heartRateSeries(String userId, Instant from, Instant to, int seconds) {
        return jdbc.query("""
                SELECT floor(extract(EPOCH FROM start_date) / ?) * ? AS bucket, avg(%s) AS bpm
                FROM health_sample
                WHERE user_id = ? AND type = ? AND start_date >= ? AND start_date < ? AND value IS NOT NULL
                GROUP BY bucket ORDER BY bucket
                """.formatted(BPM), (rs, i) -> new double[] { rs.getDouble("bucket") * 1000, rs.getDouble("bpm") },
                seconds, seconds, userId, HEART_RATE, toOffsetDateTime(from), toOffsetDateTime(to));
    }

    public List<WorkoutRow> workouts(String userId, Instant from, Instant to) {
        return jdbc.query("""
                SELECT uuid, coalesce((payload ->> 'activityType')::numeric, 3000) AS activity_type,
                    start_date, end_date, value, payload -> 'statistics' AS statistics
                FROM health_sample
                WHERE user_id = ? AND type = ? AND start_date >= ? AND start_date < ?
                ORDER BY start_date
                """, (rs, i) -> new WorkoutRow(rs.getObject("uuid", UUID.class), rs.getInt("activity_type"),
                getInstant(rs, "start_date"), getInstant(rs, "end_date"), rs.getObject("value", Double.class),
                parseJson(rs.getString("statistics"))),
                userId, WORKOUT, toOffsetDateTime(from), toOffsetDateTime(to));
    }

    /** The highest heart rate of the workouts in the range, null without any. */
    public Double maxWorkoutHeartRate(String userId, Instant from, Instant to) {
        return jdbc.queryForObject("""
                SELECT max(CASE WHEN s ->> 'unit' = 'count/s' THEN (s ->> 'maximum')::float8 * 60 ELSE (s ->> 'maximum')::float8 END)
                FROM (SELECT payload -> 'statistics' -> ? AS s FROM health_sample
                    WHERE user_id = ? AND type = ? AND start_date >= ? AND start_date < ?) w
                """, Double.class, HEART_RATE, userId, WORKOUT, toOffsetDateTime(from), toOffsetDateTime(to));
    }

    /** Value of the last sample of the type starting before {@code before}. */
    public Optional<Double> latest(String userId, String type, Instant before) {
        return jdbc.query("""
                SELECT value FROM health_sample
                WHERE user_id = ? AND type = ? AND start_date < ? AND value IS NOT NULL
                ORDER BY start_date DESC LIMIT 1
                """, (rs, i) -> rs.getDouble("value"), userId, type, toOffsetDateTime(before))
                .stream().findFirst();
    }

    public Optional<LocalDate> dateOfBirth(String userId) {
        return jdbc.query("SELECT payload ->> 'dateOfBirth' AS birth FROM health_characteristics WHERE user_id = ?",
                (rs, i) -> rs.getString("birth"), userId)
                .stream().filter(s -> s != null && s.length() >= 10).findFirst()
                .map(s -> LocalDate.parse(s.substring(0, 10)));
    }

    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }
}

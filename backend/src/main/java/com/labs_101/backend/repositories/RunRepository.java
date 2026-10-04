package com.labs_101.backend.repositories;

import static com.labs_101.backend.repositories.HealthSampleRepository.getInstant;
import static com.labs_101.backend.repositories.HealthSampleRepository.parseJson;
import static com.labs_101.backend.repositories.HealthSampleRepository.toOffsetDateTime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.JsonNode;

/**
 * Running workouts out of the synced Apple Health data (see
 * {@link HealthSampleRepository}). Routes are stored as part of the workout
 * payload, they are thinned out in the database so listing a year of runs
 * doesn't move every GPS point.
 */
@Repository
public class RunRepository {

    private static final String WORKOUT = "HKWorkoutTypeIdentifier";
    private static final String HEART_RATE = "HKQuantityTypeIdentifierHeartRate";
    /** HKWorkoutActivityType.running */
    private static final int RUNNING = 37;

    /** Columns of health_sample a workout needs, `?` is the maximum number of route points per route. */
    private static final String SELECT_RUN = """
            SELECT w.uuid, w.start_date, w.end_date, w.value, w.source_name,
                w.payload -> 'statistics' AS statistics,
                w.payload -> 'metadata' AS metadata,
                w.payload -> 'events' AS events,
                (SELECT jsonb_agg(p.pt ORDER BY (p.pt ->> 0)::float8)
                    FROM jsonb_array_elements(coalesce(w.payload -> 'routes', '[]'::jsonb)) r,
                        -- OFFSET 0 keeps the length from being computed again for every point (quadratic)
                        LATERAL (SELECT jsonb_array_length(r -> 'points') AS n OFFSET 0) l,
                        jsonb_array_elements(r -> 'points') WITH ORDINALITY p (pt, ord)
                    -- every n-th point, but always the last one so the distance is complete
                    WHERE (p.ord - 1) % greatest(1, l.n / ?) = 0 OR p.ord = l.n) AS track
            FROM health_sample w
            """;

    /** Workout, its route as [seconds since start, latitude, longitude, altitude, speed, course, accuracies...]. */
    public record RunRow(UUID uuid, Instant startDate, Instant endDate, Double duration, String sourceName,
            JsonNode statistics, JsonNode metadata, JsonNode events, JsonNode track) {
    }

    /** Heart rate in beats per minute. */
    public record HeartRateRow(Instant date, double bpm) {
    }

    /** Heart rate of a run, {@code zoneSeconds} uses the bounds passed to {@link #heartRateZones}. */
    public record ZoneRow(double[] zoneSeconds, double weightedBpm, double maxBpm) {
        public double seconds() {
            double sum = 0;
            for (double s : zoneSeconds)
                sum += s;
            return sum;
        }
    }

    private final JdbcTemplate jdbc;

    public RunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Newest first, {@code from}/{@code to} are optional. */
    public List<RunRow> findRuns(String userId, Instant from, Instant to, int maxRoutePoints) {
        List<Object> args = new ArrayList<>(List.of(maxRoutePoints));
        String sql = SELECT_RUN + " WHERE " + runFilter("w", from, to, null, userId, args) + " ORDER BY w.start_date DESC";
        return jdbc.query(sql, (rs, i) -> mapRun(rs), args.toArray());
    }

    public List<RunRow> findRun(String userId, UUID id, int maxRoutePoints) {
        List<Object> args = new ArrayList<>(List.of(maxRoutePoints));
        String sql = SELECT_RUN + " WHERE " + runFilter("w", null, null, id, userId, args);
        return jdbc.query(sql, (rs, i) -> mapRun(rs), args.toArray());
    }

    public List<HeartRateRow> heartRate(String userId, Instant from, Instant to) {
        return jdbc.query("""
                SELECT start_date, CASE WHEN unit = 'count/s' THEN value * 60 ELSE value END AS bpm
                FROM health_sample
                WHERE user_id = ? AND type = ? AND start_date >= ? AND start_date <= ? AND value IS NOT NULL
                ORDER BY start_date
                """, (rs, i) -> new HeartRateRow(getInstant(rs, "start_date"), rs.getDouble("bpm")),
                userId, HEART_RATE, toOffsetDateTime(from), toOffsetDateTime(to));
    }

    /**
     * Time per heart rate zone of every run in the range (or of the run {@code id}).
     * A sample counts until the next one, at most 30 seconds so gaps in the
     * recording don't count for the zone of the sample before them.
     *
     * @param bounds lower bounds of Z2…Z5 in bpm
     */
    public Map<UUID, ZoneRow> heartRateZones(String userId, Instant from, Instant to, UUID id, double[] bounds) {
        List<Object> args = new ArrayList<>();
        String runs = "SELECT uuid, start_date, end_date FROM health_sample w WHERE "
                + runFilter("w", from, to, id, userId, args);
        args.add(userId);
        args.add(HEART_RATE);
        // the bounds are constants of the service, not user input
        String array = Arrays.stream(bounds).mapToObj(Double::toString).collect(Collectors.joining(","));
        String sql = """
                WITH runs AS (%s),
                hr AS (
                    SELECT r.uuid AS run, h.start_date,
                        CASE WHEN h.unit = 'count/s' THEN h.value * 60 ELSE h.value END AS bpm,
                        lead(h.start_date) OVER (PARTITION BY r.uuid ORDER BY h.start_date) AS next
                    FROM runs r
                    JOIN health_sample h ON h.user_id = ? AND h.type = ?
                        AND h.start_date >= r.start_date AND h.start_date < r.end_date AND h.value IS NOT NULL
                ),
                weighted AS (
                    SELECT run, bpm, width_bucket(bpm, ARRAY[%s]::float8[]) AS zone,
                        least(extract(EPOCH FROM coalesce(next, start_date + interval '5 seconds') - start_date), 30) AS seconds
                    FROM hr
                )
                SELECT run, zone, sum(seconds) AS seconds, sum(seconds * bpm) AS weighted_bpm, max(bpm) AS max_bpm
                FROM weighted GROUP BY run, zone
                """.formatted(runs, array);

        Map<UUID, ZoneRow> zones = new HashMap<>();
        jdbc.query(sql, (RowCallbackHandler) rs -> {
            UUID run = rs.getObject("run", UUID.class);
            ZoneRow row = zones.computeIfAbsent(run, r -> new ZoneRow(new double[bounds.length + 1], 0, 0));
            double[] seconds = row.zoneSeconds();
            seconds[rs.getInt("zone")] += rs.getDouble("seconds");
            zones.put(run, new ZoneRow(seconds, row.weightedBpm() + rs.getDouble("weighted_bpm"),
                    Math.max(row.maxBpm(), rs.getDouble("max_bpm"))));
        }, args.toArray());
        return zones;
    }

    private static String runFilter(String alias, Instant from, Instant to, UUID id, String userId, List<Object> args) {
        StringBuilder sql = new StringBuilder(alias + ".user_id = ? AND " + alias + ".type = ? AND ("
                + alias + ".payload ->> 'activityType')::numeric = " + RUNNING);
        args.add(userId);
        args.add(WORKOUT);
        if (from != null) {
            sql.append(" AND ").append(alias).append(".end_date >= ?");
            args.add(toOffsetDateTime(from));
        }
        if (to != null) {
            sql.append(" AND ").append(alias).append(".start_date < ?");
            args.add(toOffsetDateTime(to));
        }
        if (id != null) {
            sql.append(" AND ").append(alias).append(".uuid = ?");
            args.add(id);
        }
        return sql.toString();
    }

    private static RunRow mapRun(ResultSet rs) throws SQLException {
        return new RunRow(
                rs.getObject("uuid", UUID.class),
                getInstant(rs, "start_date"),
                getInstant(rs, "end_date"),
                rs.getObject("value", Double.class),
                rs.getString("source_name"),
                parseJson(rs.getString("statistics")),
                parseJson(rs.getString("metadata")),
                parseJson(rs.getString("events")),
                parseJson(rs.getString("track")));
    }
}

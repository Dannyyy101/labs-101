package com.labs_101.backend.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.labs_101.backend.dtos.health.HealthSampleDto;
import com.labs_101.backend.dtos.health.HealthSampleKind;
import com.labs_101.backend.dtos.health.HealthTypeSummaryDto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain JDBC instead of JPA: the app uploads thousands of samples per request
 * and every one is an upsert ({@code ON CONFLICT}), which JPA can't do in a
 * batch. The tables are created by {@code V3__create_health_tables.sql}.
 */
@Repository
public class HealthSampleRepository {

    private static final JsonMapper JSON = new JsonMapper();

    private static final String UPSERT = """
            INSERT INTO health_sample (uuid, user_id, kind, type, start_date, end_date, value, unit,
                source_name, source_bundle_id, payload, sync_run, synced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
            ON CONFLICT (uuid) DO UPDATE SET
                kind = EXCLUDED.kind,
                type = EXCLUDED.type,
                start_date = EXCLUDED.start_date,
                end_date = EXCLUDED.end_date,
                value = EXCLUDED.value,
                unit = EXCLUDED.unit,
                source_name = EXCLUDED.source_name,
                source_bundle_id = EXCLUDED.source_bundle_id,
                payload = EXCLUDED.payload,
                sync_run = EXCLUDED.sync_run,
                synced_at = now()
            -- never touch a sample of another user
            WHERE health_sample.user_id = EXCLUDED.user_id
            """;

    private final JdbcTemplate jdbc;

    public HealthSampleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int upsert(String userId, List<HealthSampleDto> samples, UUID syncRun) {
        int[][] counts = jdbc.batchUpdate(UPSERT, samples, 500, (ps, sample) -> {
            ps.setObject(1, sample.uuid());
            ps.setString(2, userId);
            ps.setString(3, sample.kind().name());
            ps.setString(4, sample.type());
            ps.setObject(5, toOffsetDateTime(sample.startDate()));
            ps.setObject(6, toOffsetDateTime(sample.endDate()));
            ps.setObject(7, sample.value());
            ps.setString(8, sample.unit());
            ps.setString(9, sample.sourceName());
            ps.setString(10, sample.sourceBundleId());
            ps.setString(11, toJson(sample.payload()));
            ps.setObject(12, syncRun);
        });
        return sum(counts);
    }

    public int delete(String userId, List<UUID> uuids) {
        if (uuids.isEmpty())
            return 0;
        return jdbc.update("DELETE FROM health_sample WHERE user_id = ? AND uuid = ANY (?)",
                ps -> {
                    ps.setString(1, userId);
                    ps.setArray(2, ps.getConnection().createArrayOf("uuid", uuids.toArray()));
                });
    }

    /** Deletes all samples of the type that weren't uploaded by {@code syncRun}. */
    public int deleteNotInSyncRun(String userId, String type, UUID syncRun) {
        return jdbc.update(
                "DELETE FROM health_sample WHERE user_id = ? AND type = ? AND sync_run IS DISTINCT FROM ?",
                userId, type, syncRun);
    }

    public List<HealthTypeSummaryDto> summary(String userId) {
        return jdbc.query("""
                SELECT type, kind, count(*) AS count, min(start_date) AS first_date,
                    max(end_date) AS last_date, max(synced_at) AS last_synced_at
                FROM health_sample WHERE user_id = ?
                GROUP BY type, kind ORDER BY type
                """, (rs, i) -> new HealthTypeSummaryDto(
                rs.getString("type"),
                HealthSampleKind.valueOf(rs.getString("kind")),
                rs.getLong("count"),
                getInstant(rs, "first_date"),
                getInstant(rs, "last_date"),
                getInstant(rs, "last_synced_at")), userId);
    }

    /** Newest first, all filters are optional. */
    public List<HealthSampleDto> find(String userId, String type, Instant from, Instant to, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM health_sample WHERE user_id = ?");
        List<Object> args = new ArrayList<>(List.of(userId));
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type);
        }
        // overlapping the range, so a night of sleep starting the evening before is included
        if (from != null) {
            sql.append(" AND end_date >= ?");
            args.add(toOffsetDateTime(from));
        }
        if (to != null) {
            sql.append(" AND start_date < ?");
            args.add(toOffsetDateTime(to));
        }
        sql.append(" ORDER BY start_date DESC, uuid LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), HealthSampleRepository::mapSample, args.toArray());
    }

    public void saveCharacteristics(String userId, JsonNode payload) {
        jdbc.update("""
                INSERT INTO health_characteristics (user_id, payload, updated_at) VALUES (?, CAST(? AS jsonb), now())
                ON CONFLICT (user_id) DO UPDATE SET payload = EXCLUDED.payload, updated_at = now()
                """, userId, toJson(payload));
    }

    public Optional<JsonNode> findCharacteristics(String userId) {
        return jdbc.query("SELECT payload FROM health_characteristics WHERE user_id = ?",
                (rs, i) -> parseJson(rs.getString("payload")), userId).stream().findFirst();
    }

    private static HealthSampleDto mapSample(ResultSet rs, int rowNum) throws SQLException {
        return new HealthSampleDto(
                rs.getObject("uuid", UUID.class),
                HealthSampleKind.valueOf(rs.getString("kind")),
                rs.getString("type"),
                getInstant(rs, "start_date"),
                getInstant(rs, "end_date"),
                rs.getObject("value", Double.class),
                rs.getString("unit"),
                rs.getString("source_name"),
                rs.getString("source_bundle_id"),
                parseJson(rs.getString("payload")));
    }

    static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    static Instant getInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    static String toJson(JsonNode node) {
        return node == null || node.isNull() ? null : JSON.writeValueAsString(node);
    }

    static JsonNode parseJson(String json) {
        return json == null ? null : JSON.readTree(json);
    }

    private static int sum(int[][] counts) {
        return Arrays.stream(counts).flatMapToInt(Arrays::stream)
                // the driver may report SUCCESS_NO_INFO for batched statements
                .map(count -> count == Statement.SUCCESS_NO_INFO ? 1 : count)
                .sum();
    }
}

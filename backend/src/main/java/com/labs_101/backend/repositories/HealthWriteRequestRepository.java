package com.labs_101.backend.repositories;

import static com.labs_101.backend.repositories.HealthSampleRepository.getInstant;
import static com.labs_101.backend.repositories.HealthSampleRepository.parseJson;
import static com.labs_101.backend.repositories.HealthSampleRepository.toJson;
import static com.labs_101.backend.repositories.HealthSampleRepository.toOffsetDateTime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.labs_101.backend.dtos.health.CreateHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.HealthSampleKind;
import com.labs_101.backend.dtos.health.HealthWriteRequestDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestStatus;

@Repository
public class HealthWriteRequestRepository {

    private final JdbcTemplate jdbc;

    public HealthWriteRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HealthWriteRequestDto create(String userId, CreateHealthWriteRequestDto dto) {
        return jdbc.queryForObject("""
                INSERT INTO health_write_request (user_id, kind, type, start_date, end_date, value, unit, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                RETURNING *
                """, HealthWriteRequestRepository::map,
                userId, dto.kind().name(), dto.type(), toOffsetDateTime(dto.startDate()),
                toOffsetDateTime(dto.endDate()), dto.value(), dto.unit(), toJson(dto.metadata()));
    }

    /** Oldest first, so the app writes them in the order they were created. */
    public List<HealthWriteRequestDto> findByStatus(String userId, HealthWriteRequestStatus status) {
        return jdbc.query("SELECT * FROM health_write_request WHERE user_id = ? AND status = ? ORDER BY id",
                HealthWriteRequestRepository::map, userId, status.name());
    }

    public Optional<HealthWriteRequestDto> updateStatus(String userId, Long id, HealthWriteRequestStatus status,
            UUID sampleUuid, String error) {
        return jdbc.query("""
                UPDATE health_write_request SET status = ?, sample_uuid = ?, error = ?, updated_at = now()
                WHERE user_id = ? AND id = ?
                RETURNING *
                """, HealthWriteRequestRepository::map, status.name(), sampleUuid, error, userId, id)
                .stream().findFirst();
    }

    private static HealthWriteRequestDto map(ResultSet rs, int rowNum) throws SQLException {
        return new HealthWriteRequestDto(
                rs.getLong("id"),
                HealthSampleKind.valueOf(rs.getString("kind")),
                rs.getString("type"),
                getInstant(rs, "start_date"),
                getInstant(rs, "end_date"),
                rs.getObject("value", Double.class),
                rs.getString("unit"),
                parseJson(rs.getString("metadata")),
                HealthWriteRequestStatus.valueOf(rs.getString("status")),
                rs.getObject("sample_uuid", UUID.class),
                rs.getString("error"),
                getInstant(rs, "created_at"));
    }
}

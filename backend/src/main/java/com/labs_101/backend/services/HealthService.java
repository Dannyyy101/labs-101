package com.labs_101.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.dtos.health.AckHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.CompleteFullSyncDto;
import com.labs_101.backend.dtos.health.CreateHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.DeleteHealthSamplesDto;
import com.labs_101.backend.dtos.health.HealthSampleDto;
import com.labs_101.backend.dtos.health.HealthSampleKind;
import com.labs_101.backend.dtos.health.HealthSyncResultDto;
import com.labs_101.backend.dtos.health.HealthTypeSummaryDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestStatus;
import com.labs_101.backend.dtos.health.UploadHealthSamplesDto;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.HealthSampleRepository;
import com.labs_101.backend.repositories.HealthWriteRequestRepository;

import tools.jackson.databind.JsonNode;

/**
 * Mirror of the Apple Health data of a user.
 *
 * The app pushes changes with HealthKit anchored queries: new or changed
 * samples are upserted by their HealthKit UUID and deleted samples are deleted
 * by UUID. Without its anchors (e.g. after reinstalling the app) the app syncs
 * a type from the beginning, tagging every batch with the same full sync id.
 * Because of the upsert this never creates duplicates, and completing the run
 * removes samples that were deleted in HealthKit while the app had no anchor.
 */
@Service
public class HealthService {

    public static final int MAX_PAGE_SIZE = 1000;

    private final HealthSampleRepository sampleRepository;
    private final HealthWriteRequestRepository writeRequestRepository;

    HealthService(HealthSampleRepository sampleRepository, HealthWriteRequestRepository writeRequestRepository) {
        this.sampleRepository = sampleRepository;
        this.writeRequestRepository = writeRequestRepository;
    }

    @Transactional
    public HealthSyncResultDto upload(String userId, UploadHealthSamplesDto dto) {
        List<HealthSampleDto> samples = dto.samples() == null ? List.of() : dto.samples();
        samples.forEach(HealthService::validate);
        return new HealthSyncResultDto(sampleRepository.upsert(userId, samples, dto.fullSyncId()));
    }

    @Transactional
    public HealthSyncResultDto delete(String userId, DeleteHealthSamplesDto dto) {
        return new HealthSyncResultDto(sampleRepository.delete(userId, dto.uuids() == null ? List.of() : dto.uuids()));
    }

    @Transactional
    public HealthSyncResultDto completeFullSync(String userId, CompleteFullSyncDto dto) {
        if (dto.type() == null || dto.type().isBlank() || dto.fullSyncId() == null)
            throw badRequest("type and fullSyncId are required");
        return new HealthSyncResultDto(sampleRepository.deleteNotInSyncRun(userId, dto.type(), dto.fullSyncId()));
    }

    public List<HealthTypeSummaryDto> getSummary(String userId) {
        return sampleRepository.summary(userId);
    }

    public List<HealthSampleDto> getSamples(String userId, String type, Instant from, Instant to, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE)
            throw badRequest("page has to be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        return sampleRepository.find(userId, type, from, to, size, page * size);
    }

    public void saveCharacteristics(String userId, JsonNode payload) {
        if (payload == null || !payload.isObject())
            throw badRequest("characteristics have to be a JSON object");
        sampleRepository.saveCharacteristics(userId, payload);
    }

    public Optional<JsonNode> getCharacteristics(String userId) {
        return sampleRepository.findCharacteristics(userId);
    }

    public HealthWriteRequestDto createWriteRequest(String userId, CreateHealthWriteRequestDto dto) {
        if (dto.kind() != HealthSampleKind.QUANTITY && dto.kind() != HealthSampleKind.CATEGORY)
            throw badRequest("only QUANTITY and CATEGORY samples can be written");
        if (dto.type() == null || dto.type().isBlank() || dto.startDate() == null || dto.endDate() == null
                || dto.value() == null)
            throw badRequest("type, startDate, endDate and value are required");
        if (dto.kind() == HealthSampleKind.QUANTITY && (dto.unit() == null || dto.unit().isBlank()))
            throw badRequest("quantity samples need a unit");
        if (dto.endDate().isBefore(dto.startDate()))
            throw badRequest("endDate is before startDate");
        return writeRequestRepository.create(userId, dto);
    }

    public List<HealthWriteRequestDto> getWriteRequests(String userId, HealthWriteRequestStatus status) {
        return writeRequestRepository.findByStatus(userId, status);
    }

    public HealthWriteRequestDto acknowledgeWriteRequest(String userId, Long id, AckHealthWriteRequestDto dto) {
        boolean failed = dto.sampleUuid() == null;
        return writeRequestRepository.updateStatus(userId, id,
                failed ? HealthWriteRequestStatus.FAILED : HealthWriteRequestStatus.WRITTEN,
                dto.sampleUuid(), failed ? Optional.ofNullable(dto.error()).orElse("unknown error") : null)
                .orElseThrow(() -> NotFoundException.healthWriteRequest(id));
    }

    private static void validate(HealthSampleDto sample) {
        if (sample.uuid() == null || sample.kind() == null || sample.type() == null || sample.startDate() == null
                || sample.endDate() == null)
            throw badRequest("uuid, kind, type, startDate and endDate are required for every sample");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

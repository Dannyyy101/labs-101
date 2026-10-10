package com.labs_101.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.health.AckHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.CompleteFullSyncDto;
import com.labs_101.backend.dtos.health.CreateHealthWriteRequestDto;
import com.labs_101.backend.dtos.health.DeleteHealthSamplesDto;
import com.labs_101.backend.dtos.health.HealthSampleDto;
import com.labs_101.backend.dtos.health.HealthSyncResultDto;
import com.labs_101.backend.dtos.health.HealthTypeSummaryDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestDto;
import com.labs_101.backend.dtos.health.HealthWriteRequestStatus;
import com.labs_101.backend.dtos.health.UploadHealthSamplesDto;
import com.labs_101.backend.services.HealthService;
import com.labs_101.backend.security.CurrentUser;

import tools.jackson.databind.JsonNode;

@RestController()
@RequestMapping("/api/users/me/health")
public class HealthController {

    private final HealthService healthService;

    HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // MARK: sync from the app

    @PostMapping("/samples")
    public HealthSyncResultDto uploadSamples(@CurrentUser String userId, @RequestBody UploadHealthSamplesDto dto) {
        return healthService.upload(userId, dto);
    }

    @PostMapping("/samples/delete")
    public HealthSyncResultDto deleteSamples(@CurrentUser String userId, @RequestBody DeleteHealthSamplesDto dto) {
        return healthService.delete(userId, dto);
    }

    @PostMapping("/sync/complete")
    public HealthSyncResultDto completeFullSync(@CurrentUser String userId, @RequestBody CompleteFullSyncDto dto) {
        return healthService.completeFullSync(userId, dto);
    }

    @PutMapping("/characteristics")
    public ResponseEntity<Void> saveCharacteristics(@CurrentUser String userId, @RequestBody JsonNode payload) {
        healthService.saveCharacteristics(userId, payload);
        return ResponseEntity.noContent().build();
    }

    // MARK: reading

    @GetMapping("/characteristics")
    public ResponseEntity<JsonNode> getCharacteristics(@CurrentUser String userId) {
        return ResponseEntity.of(healthService.getCharacteristics(userId));
    }

    @GetMapping("/types")
    public List<HealthTypeSummaryDto> getSummary(@CurrentUser String userId) {
        return healthService.getSummary(userId);
    }

    @GetMapping("/samples")
    public List<HealthSampleDto> getSamples(@CurrentUser String userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        return healthService.getSamples(userId, type, from, to, page, size);
    }

    // MARK: writing into HealthKit (picked up by the app on its next sync)

    @PostMapping("/write-requests")
    public HealthWriteRequestDto createWriteRequest(@CurrentUser String userId,
            @RequestBody CreateHealthWriteRequestDto dto) {
        return healthService.createWriteRequest(userId, dto);
    }

    @GetMapping("/write-requests")
    public List<HealthWriteRequestDto> getWriteRequests(@CurrentUser String userId,
            @RequestParam(defaultValue = "PENDING") HealthWriteRequestStatus status) {
        return healthService.getWriteRequests(userId, status);
    }

    @PostMapping("/write-requests/{id}/ack")
    public HealthWriteRequestDto acknowledgeWriteRequest(@CurrentUser String userId, @PathVariable Long id,
            @RequestBody AckHealthWriteRequestDto dto) {
        return healthService.acknowledgeWriteRequest(userId, id, dto);
    }
}

package com.cliniq.controller;

import com.cliniq.dto.QueueDisplayResponse;
import com.cliniq.dto.StatusUpdateRequest;
import com.cliniq.dto.VisitResponse;
import com.cliniq.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final QueueService queueService;

    // Public endpoint - no auth required (US5)
    @GetMapping("/display")
    public ResponseEntity<QueueDisplayResponse> getQueueDisplay() {
        log.debug("Public queue display requested");
        return ResponseEntity.ok(queueService.getQueueDisplay());
    }

    // Staff endpoint - requires RECEPTIONIST or DOCTOR role
    @GetMapping("/today")
    public ResponseEntity<List<VisitResponse>> getTodayQueue() {
        log.debug("Today's queue requested by staff");
        return ResponseEntity.ok(queueService.getTodayQueue());
    }

    // Doctor only - call next patient (US4)
    @PostMapping("/manage/call-next")
    public ResponseEntity<VisitResponse> callNextPatient() {
        log.info("Call next patient requested");
        return ResponseEntity.ok(queueService.callNextPatient());
    }

    // Doctor only - update visit status (US4)
    @PatchMapping("/manage/visits/{id}/status")
    public ResponseEntity<VisitResponse> updateVisitStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        log.info("Status update requested for visit {}: {}", id, request.getStatus());
        return ResponseEntity.ok(queueService.updateVisitStatus(id, request.getStatus()));
    }
}

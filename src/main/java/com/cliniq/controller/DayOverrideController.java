package com.cliniq.controller;

import com.cliniq.dto.DayOverrideRequest;
import com.cliniq.dto.DayOverrideResponse;
import com.cliniq.service.DayOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/day-overrides")
@RequiredArgsConstructor
public class DayOverrideController {

    private final DayOverrideService dayOverrideService;

    @GetMapping
    public ResponseEntity<List<DayOverrideResponse>> getAllOverrides() {
        return ResponseEntity.ok(dayOverrideService.getAllOverrides());
    }

    @GetMapping("/date")
    public ResponseEntity<DayOverrideResponse> getOverrideByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dayOverrideService.getOverrideByDate(date));
    }

    @GetMapping("/range")
    public ResponseEntity<List<DayOverrideResponse>> getOverridesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dayOverrideService.getOverridesByRange(startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<DayOverrideResponse> createOrUpdateOverride(
            @Valid @RequestBody DayOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dayOverrideService.createOrUpdateOverride(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOverride(@PathVariable Long id) {
        dayOverrideService.deleteOverride(id);
        return ResponseEntity.ok(Map.of("message", "Override deleted successfully"));
    }

    @DeleteMapping("/date")
    public ResponseEntity<Map<String, String>> deleteOverrideByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dayOverrideService.deleteOverrideByDate(date);
        return ResponseEntity.ok(Map.of("message", "Override deleted successfully"));
    }
}

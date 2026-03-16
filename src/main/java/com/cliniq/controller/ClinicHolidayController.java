package com.cliniq.controller;

import com.cliniq.dto.ClinicHolidayRequest;
import com.cliniq.dto.ClinicHolidayResponse;
import com.cliniq.service.ClinicHolidayService;
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
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class ClinicHolidayController {

    private final ClinicHolidayService clinicHolidayService;

    @GetMapping
    public ResponseEntity<List<ClinicHolidayResponse>> getAllHolidays() {
        return ResponseEntity.ok(clinicHolidayService.getAllHolidays());
    }

    @GetMapping("/range")
    public ResponseEntity<List<ClinicHolidayResponse>> getHolidaysByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(clinicHolidayService.getHolidaysByRange(startDate, endDate));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(Map.of("isHoliday", clinicHolidayService.isHoliday(date)));
    }

    @PostMapping
    public ResponseEntity<ClinicHolidayResponse> addHoliday(
            @Valid @RequestBody ClinicHolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicHolidayService.addHoliday(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicHolidayResponse> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody ClinicHolidayRequest request) {
        return ResponseEntity.ok(clinicHolidayService.updateHoliday(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHoliday(@PathVariable Long id) {
        clinicHolidayService.deleteHoliday(id);
        return ResponseEntity.ok(Map.of("message", "Holiday deleted successfully"));
    }
}

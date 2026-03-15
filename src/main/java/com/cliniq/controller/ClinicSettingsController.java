package com.cliniq.controller;

import com.cliniq.dto.ClinicSettingsRequest;
import com.cliniq.dto.ClinicSettingsResponse;
import com.cliniq.service.ClinicSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class ClinicSettingsController {

    private final ClinicSettingsService clinicSettingsService;

    @GetMapping
    public ResponseEntity<ClinicSettingsResponse> getSettings() {
        return ResponseEntity.ok(clinicSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<ClinicSettingsResponse> updateSettings(
            @Valid @RequestBody ClinicSettingsRequest request) {
        return ResponseEntity.ok(clinicSettingsService.updateSettings(request));
    }
}

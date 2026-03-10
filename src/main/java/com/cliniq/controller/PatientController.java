package com.cliniq.controller;

import com.cliniq.dto.PatientRegistrationRequest;
import com.cliniq.dto.PatientRegistrationResponse;
import com.cliniq.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    @PostMapping("/register")
    public ResponseEntity<PatientRegistrationResponse> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {
        log.info("Received patient registration request for: {}", request.getName());
        PatientRegistrationResponse response = patientService.registerWalkInPatient(request);
        log.info("Patient registered with token: {}", response.getTokenNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package com.cliniq.service;

import com.cliniq.dto.PatientRegistrationRequest;
import com.cliniq.dto.PatientRegistrationResponse;
import com.cliniq.entity.Patient;
import com.cliniq.entity.Visit;
import com.cliniq.repository.PatientRepository;
import com.cliniq.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    @Transactional
    public PatientRegistrationResponse registerWalkInPatient(PatientRegistrationRequest request) {
        log.info("Registering walk-in patient: {} with phone: {}", request.getName(), request.getPhone());

        // Find existing patient or create new one
        Patient patient = patientRepository.findByPhoneAndName(request.getPhone(), request.getName())
                .orElseGet(() -> {
                    log.info("Creating new patient record for: {}", request.getName());
                    Patient newPatient = Patient.builder()
                            .name(request.getName())
                            .phone(request.getPhone())
                            .build();
                    return patientRepository.save(newPatient);
                });

        log.debug("Patient ID: {}", patient.getId());

        // Check if patient already has a visit today
        LocalDate today = LocalDate.now();
        if (visitRepository.existsByPatientIdAndVisitDate(patient.getId(), today)) {
            log.warn("Patient {} already has a visit today", patient.getName());
            throw new IllegalStateException("Patient " + patient.getName() + " already has a visit registered for today");
        }

        // Generate next sequential token number for today
        Integer maxToken = visitRepository.findMaxTokenNumberByDate(today).orElse(0);
        int nextToken = maxToken + 1;
        log.info("Generated token number: {} for date: {}", nextToken, today);

        // Create visit
        Visit visit = Visit.builder()
                .tokenNumber(nextToken)
                .visitDate(today)
                .patient(patient)
                .build();
        visit = visitRepository.save(visit);

        log.info("Visit created successfully. Visit ID: {}, Token: {}", visit.getId(), visit.getTokenNumber());

        return PatientRegistrationResponse.builder()
                .visitId(visit.getId())
                .tokenNumber(visit.getTokenNumber())
                .patientName(patient.getName())
                .phone(patient.getPhone())
                .visitDate(visit.getVisitDate())
                .status(visit.getStatus().name())
                .message("Patient registered successfully. Token number: " + nextToken)
                .build();
    }
}

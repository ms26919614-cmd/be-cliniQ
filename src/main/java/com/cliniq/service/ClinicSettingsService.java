package com.cliniq.service;

import com.cliniq.dto.ClinicSettingsRequest;
import com.cliniq.dto.ClinicSettingsResponse;
import com.cliniq.entity.ClinicSettings;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ClinicSettingsService {

    private final ClinicSettingsRepository clinicSettingsRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ClinicSettingsResponse getSettings() {
        ClinicSettings settings = clinicSettingsRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Clinic settings not configured. Please configure settings first."));
        return mapToResponse(settings);
    }

    @Transactional
    public ClinicSettingsResponse updateSettings(ClinicSettingsRequest request) {
        LocalTime startTime = LocalTime.parse(request.getWorkingStartTime(), TIME_FMT);
        LocalTime endTime = LocalTime.parse(request.getWorkingEndTime(), TIME_FMT);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Working end time must be after start time");
        }

        if (request.getBreakStartTime() != null && request.getBreakEndTime() != null) {
            LocalTime breakStart = LocalTime.parse(request.getBreakStartTime(), TIME_FMT);
            LocalTime breakEnd = LocalTime.parse(request.getBreakEndTime(), TIME_FMT);
            if (!breakEnd.isAfter(breakStart)) {
                throw new IllegalArgumentException("Break end time must be after break start time");
            }
            if (breakStart.isBefore(startTime) || breakEnd.isAfter(endTime)) {
                throw new IllegalArgumentException("Break time must be within working hours");
            }
        }

        ClinicSettings settings = clinicSettingsRepository.findFirstByOrderByIdAsc()
                .orElse(new ClinicSettings());

        settings.setClinicName(request.getClinicName());
        settings.setWorkingStartTime(startTime);
        settings.setWorkingEndTime(endTime);
        settings.setSlotDurationMinutes(request.getSlotDurationMinutes());
        settings.setMaxPatientsPerSlot(request.getMaxPatientsPerSlot());

        if (request.getBreakStartTime() != null) {
            settings.setBreakStartTime(LocalTime.parse(request.getBreakStartTime(), TIME_FMT));
        } else {
            settings.setBreakStartTime(null);
        }

        if (request.getBreakEndTime() != null) {
            settings.setBreakEndTime(LocalTime.parse(request.getBreakEndTime(), TIME_FMT));
        } else {
            settings.setBreakEndTime(null);
        }

        ClinicSettings saved = clinicSettingsRepository.save(settings);
        return mapToResponse(saved);
    }

    private ClinicSettingsResponse mapToResponse(ClinicSettings settings) {
        return ClinicSettingsResponse.builder()
                .id(settings.getId())
                .clinicName(settings.getClinicName())
                .workingStartTime(settings.getWorkingStartTime().format(TIME_FMT))
                .workingEndTime(settings.getWorkingEndTime().format(TIME_FMT))
                .slotDurationMinutes(settings.getSlotDurationMinutes())
                .maxPatientsPerSlot(settings.getMaxPatientsPerSlot())
                .breakStartTime(settings.getBreakStartTime() != null ? settings.getBreakStartTime().format(TIME_FMT) : null)
                .breakEndTime(settings.getBreakEndTime() != null ? settings.getBreakEndTime().format(TIME_FMT) : null)
                .updatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt().format(DATETIME_FMT) : null)
                .build();
    }
}

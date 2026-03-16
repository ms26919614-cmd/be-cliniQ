package com.cliniq.service;

import com.cliniq.dto.DayOverrideRequest;
import com.cliniq.dto.DayOverrideResponse;
import com.cliniq.entity.DayOverride;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.DayOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DayOverrideService {

    private final DayOverrideRepository dayOverrideRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public List<DayOverrideResponse> getAllOverrides() {
        return dayOverrideRepository.findAllByOrderByOverrideDateAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DayOverrideResponse getOverrideByDate(LocalDate date) {
        DayOverride override = dayOverrideRepository.findByOverrideDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("No override found for date: " + date));
        return mapToResponse(override);
    }

    public Optional<DayOverride> findByDate(LocalDate date) {
        return dayOverrideRepository.findByOverrideDate(date);
    }

    public List<DayOverrideResponse> getOverridesByRange(LocalDate startDate, LocalDate endDate) {
        return dayOverrideRepository.findByOverrideDateBetweenOrderByOverrideDateAsc(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DayOverrideResponse createOrUpdateOverride(DayOverrideRequest request) {
        LocalDate date = LocalDate.parse(request.getOverrideDate(), DATE_FMT);

        DayOverride override = dayOverrideRepository.findByOverrideDate(date)
                .orElse(DayOverride.builder().overrideDate(date).build());

        override.setWorkingDay(request.getWorkingDay());
        override.setReason(request.getReason() != null ? request.getReason() : "");

        if (request.getWorkingDay() && request.getStartTime() != null && request.getEndTime() != null) {
            override.setStartTime(LocalTime.parse(request.getStartTime(), TIME_FMT));
            override.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FMT));
        } else if (!request.getWorkingDay()) {
            override.setStartTime(null);
            override.setEndTime(null);
        }

        DayOverride saved = dayOverrideRepository.save(override);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOverride(Long id) {
        if (!dayOverrideRepository.existsById(id)) {
            throw new ResourceNotFoundException("Override not found with id: " + id);
        }
        dayOverrideRepository.deleteById(id);
    }

    @Transactional
    public void deleteOverrideByDate(LocalDate date) {
        DayOverride override = dayOverrideRepository.findByOverrideDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("No override found for date: " + date));
        dayOverrideRepository.delete(override);
    }

    private DayOverrideResponse mapToResponse(DayOverride override) {
        return DayOverrideResponse.builder()
                .id(override.getId())
                .overrideDate(override.getOverrideDate().format(DATE_FMT))
                .workingDay(override.isWorkingDay())
                .startTime(override.getStartTime() != null ? override.getStartTime().format(TIME_FMT) : null)
                .endTime(override.getEndTime() != null ? override.getEndTime().format(TIME_FMT) : null)
                .reason(override.getReason())
                .createdAt(override.getCreatedAt() != null ? override.getCreatedAt().format(DATETIME_FMT) : null)
                .updatedAt(override.getUpdatedAt() != null ? override.getUpdatedAt().format(DATETIME_FMT) : null)
                .build();
    }
}

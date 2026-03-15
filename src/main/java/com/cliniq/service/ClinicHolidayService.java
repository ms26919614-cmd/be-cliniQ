package com.cliniq.service;

import com.cliniq.dto.ClinicHolidayRequest;
import com.cliniq.dto.ClinicHolidayResponse;
import com.cliniq.entity.ClinicHoliday;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.ClinicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicHolidayService {

    private final ClinicHolidayRepository clinicHolidayRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<ClinicHolidayResponse> getAllHolidays() {
        return clinicHolidayRepository.findAllByOrderByHolidayDateAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ClinicHolidayResponse> getHolidaysByRange(LocalDate startDate, LocalDate endDate) {
        return clinicHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean isHoliday(LocalDate date) {
        return clinicHolidayRepository.existsByHolidayDate(date);
    }

    @Transactional
    public ClinicHolidayResponse addHoliday(ClinicHolidayRequest request) {
        LocalDate date = LocalDate.parse(request.getHolidayDate(), DATE_FMT);

        if (clinicHolidayRepository.existsByHolidayDate(date)) {
            throw new IllegalArgumentException("Holiday already exists for date: " + request.getHolidayDate());
        }

        ClinicHoliday holiday = ClinicHoliday.builder()
                .holidayDate(date)
                .description(request.getDescription() != null ? request.getDescription() : "")
                .build();

        ClinicHoliday saved = clinicHolidayRepository.save(holiday);
        return mapToResponse(saved);
    }

    @Transactional
    public ClinicHolidayResponse updateHoliday(Long id, ClinicHolidayRequest request) {
        ClinicHoliday holiday = clinicHolidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));

        LocalDate newDate = LocalDate.parse(request.getHolidayDate(), DATE_FMT);

        // Check if date is being changed and new date already exists
        if (!holiday.getHolidayDate().equals(newDate) && clinicHolidayRepository.existsByHolidayDate(newDate)) {
            throw new IllegalArgumentException("Holiday already exists for date: " + request.getHolidayDate());
        }

        holiday.setHolidayDate(newDate);
        holiday.setDescription(request.getDescription() != null ? request.getDescription() : "");

        ClinicHoliday saved = clinicHolidayRepository.save(holiday);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        if (!clinicHolidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday not found with id: " + id);
        }
        clinicHolidayRepository.deleteById(id);
    }

    private ClinicHolidayResponse mapToResponse(ClinicHoliday holiday) {
        return ClinicHolidayResponse.builder()
                .id(holiday.getId())
                .holidayDate(holiday.getHolidayDate().format(DATE_FMT))
                .description(holiday.getDescription())
                .createdAt(holiday.getCreatedAt() != null ? holiday.getCreatedAt().format(DATETIME_FMT) : null)
                .build();
    }
}

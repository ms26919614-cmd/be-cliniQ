package com.cliniq.service;

import com.cliniq.dto.*;
import com.cliniq.entity.AppointmentSlot;
import com.cliniq.entity.ClinicSettings;
import com.cliniq.enums.DayOfWeekEnum;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.AppointmentSlotRepository;
import com.cliniq.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentSlotService {

    private final AppointmentSlotRepository appointmentSlotRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<AppointmentSlotResponse> getAllSlots() {
        return appointmentSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentSlotResponse> getSlotsByDay(String dayOfWeek) {
        DayOfWeekEnum day = parseDayOfWeek(dayOfWeek);
        return appointmentSlotRepository.findByDayOfWeekOrderByStartTimeAsc(day)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentSlotResponse> getActiveSlotsByDay(String dayOfWeek) {
        DayOfWeekEnum day = parseDayOfWeek(dayOfWeek);
        return appointmentSlotRepository.findByDayOfWeekAndActiveOrderByStartTimeAsc(day, true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentSlotResponse> getAvailableSlotsForDate(LocalDate date) {
        DayOfWeekEnum day = DayOfWeekEnum.valueOf(date.getDayOfWeek().name());
        return appointmentSlotRepository.findByDayOfWeekAndActiveOrderByStartTimeAsc(day, true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentSlotResponse createSlot(AppointmentSlotRequest request) {
        DayOfWeekEnum day = parseDayOfWeek(request.getDayOfWeek());
        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FMT);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FMT);

        validateSlotTimes(startTime, endTime);

        if (appointmentSlotRepository.existsByDayOfWeekAndStartTime(day, startTime)) {
            throw new IllegalArgumentException(
                    "A slot already exists for " + day + " at " + request.getStartTime());
        }

        AppointmentSlot slot = AppointmentSlot.builder()
                .dayOfWeek(day)
                .startTime(startTime)
                .endTime(endTime)
                .maxPatients(request.getMaxPatients())
                .active(true)
                .build();

        AppointmentSlot saved = appointmentSlotRepository.save(slot);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentSlotResponse updateSlot(Long slotId, AppointmentSlotRequest request) {
        AppointmentSlot slot = appointmentSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + slotId));

        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FMT);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FMT);
        DayOfWeekEnum day = parseDayOfWeek(request.getDayOfWeek());

        validateSlotTimes(startTime, endTime);

        slot.setDayOfWeek(day);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setMaxPatients(request.getMaxPatients());

        AppointmentSlot saved = appointmentSlotRepository.save(slot);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentSlotResponse toggleSlotActive(Long slotId) {
        AppointmentSlot slot = appointmentSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + slotId));

        slot.setActive(!slot.isActive());
        AppointmentSlot saved = appointmentSlotRepository.save(slot);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteSlot(Long slotId) {
        if (!appointmentSlotRepository.existsById(slotId)) {
            throw new ResourceNotFoundException("Appointment slot not found with id: " + slotId);
        }
        appointmentSlotRepository.deleteById(slotId);
    }

    @Transactional
    public List<AppointmentSlotResponse> generateSlots(GenerateSlotsRequest request) {
        ClinicSettings settings = clinicSettingsRepository.findFirstByOrderByIdAsc().orElse(null);

        String startTimeStr = request.getStartTime();
        String endTimeStr = request.getEndTime();
        Integer duration = request.getSlotDurationMinutes();
        Integer maxPatients = request.getMaxPatientsPerSlot();

        // Fall back to clinic settings if not provided in request
        if (settings != null) {
            if (startTimeStr == null) startTimeStr = settings.getWorkingStartTime().format(TIME_FMT);
            if (endTimeStr == null) endTimeStr = settings.getWorkingEndTime().format(TIME_FMT);
            if (duration == null) duration = settings.getSlotDurationMinutes();
            if (maxPatients == null) maxPatients = settings.getMaxPatientsPerSlot();
        }

        if (startTimeStr == null || endTimeStr == null || duration == null || maxPatients == null) {
            throw new IllegalArgumentException(
                    "Slot generation requires start time, end time, duration, and max patients. " +
                    "Configure clinic settings or provide values in the request.");
        }

        LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FMT);
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FMT);
        LocalTime breakStart = settings != null && settings.getBreakStartTime() != null ? settings.getBreakStartTime() : null;
        LocalTime breakEnd = settings != null && settings.getBreakEndTime() != null ? settings.getBreakEndTime() : null;

        List<AppointmentSlot> generatedSlots = new ArrayList<>();

        for (String dayStr : request.getDaysOfWeek()) {
            DayOfWeekEnum day = parseDayOfWeek(dayStr);
            LocalTime currentTime = startTime;

            while (currentTime.plusMinutes(duration).compareTo(endTime) <= 0) {
                LocalTime slotEnd = currentTime.plusMinutes(duration);

                // Skip break time
                if (breakStart != null && breakEnd != null) {
                    if (!currentTime.isBefore(breakStart) && currentTime.isBefore(breakEnd)) {
                        currentTime = breakEnd;
                        continue;
                    }
                    if (currentTime.isBefore(breakStart) && slotEnd.isAfter(breakStart)) {
                        currentTime = breakEnd;
                        continue;
                    }
                }

                // Only create if slot doesn't already exist
                if (!appointmentSlotRepository.existsByDayOfWeekAndStartTime(day, currentTime)) {
                    AppointmentSlot slot = AppointmentSlot.builder()
                            .dayOfWeek(day)
                            .startTime(currentTime)
                            .endTime(slotEnd)
                            .maxPatients(maxPatients)
                            .active(true)
                            .build();
                    generatedSlots.add(slot);
                }

                currentTime = slotEnd;
            }
        }

        List<AppointmentSlot> savedSlots = appointmentSlotRepository.saveAll(generatedSlots);
        return savedSlots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateSlotTimes(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private DayOfWeekEnum parseDayOfWeek(String day) {
        try {
            return DayOfWeekEnum.valueOf(day.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid day of week: '" + day + "'. Must be one of: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
        }
    }

    private AppointmentSlotResponse mapToResponse(AppointmentSlot slot) {
        return AppointmentSlotResponse.builder()
                .id(slot.getId())
                .dayOfWeek(slot.getDayOfWeek().name())
                .startTime(slot.getStartTime().format(TIME_FMT))
                .endTime(slot.getEndTime().format(TIME_FMT))
                .maxPatients(slot.getMaxPatients())
                .active(slot.isActive())
                .createdAt(slot.getCreatedAt() != null ? slot.getCreatedAt().format(DATETIME_FMT) : null)
                .updatedAt(slot.getUpdatedAt() != null ? slot.getUpdatedAt().format(DATETIME_FMT) : null)
                .build();
    }
}

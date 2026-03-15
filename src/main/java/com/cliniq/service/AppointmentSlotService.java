package com.cliniq.service;

import com.cliniq.dto.*;
import com.cliniq.entity.*;
import com.cliniq.enums.DayOfWeekEnum;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentSlotService {

    private final AppointmentSlotRepository appointmentSlotRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;
    private final AppointmentRepository appointmentRepository;
    private final DayScheduleRepository dayScheduleRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final DayOverrideRepository dayOverrideRepository;

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
        // Check if date is a holiday
        if (clinicHolidayRepository.existsByHolidayDate(date)) {
            return new ArrayList<>(); // No slots available on holidays
        }

        DayOfWeekEnum day = DayOfWeekEnum.valueOf(date.getDayOfWeek().name());

        // Check for a date-specific override first
        Optional<DayOverride> overrideOpt = dayOverrideRepository.findByOverrideDate(date);
        if (overrideOpt.isPresent()) {
            DayOverride override = overrideOpt.get();
            if (!override.isWorkingDay()) {
                return new ArrayList<>(); // Override marks this date as non-working
            }
            // Override marks it as working — fall through to return slots
        } else {
            // No override — use weekly schedule
            Optional<DaySchedule> dayScheduleOpt = dayScheduleRepository.findByDayOfWeek(day);
            if (dayScheduleOpt.isPresent() && !dayScheduleOpt.get().isWorkingDay()) {
                return new ArrayList<>(); // Non-working day per weekly schedule
            }
        }

        return appointmentSlotRepository.findByDayOfWeekAndActiveOrderByStartTimeAsc(day, true)
                .stream()
                .map(slot -> {
                    AppointmentSlotResponse response = mapToResponse(slot);
                    long booked = appointmentRepository.countActiveBookingsForSlot(slot.getId(), date);
                    response.setBookedCount(booked);
                    return response;
                })
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

        Integer duration = request.getSlotDurationMinutes();
        Integer maxPatients = request.getMaxPatientsPerSlot();

        // Fall back to clinic settings for duration and maxPatients if not provided
        if (settings != null) {
            if (duration == null) duration = settings.getSlotDurationMinutes();
            if (maxPatients == null) maxPatients = settings.getMaxPatientsPerSlot();
        }

        if (duration == null || maxPatients == null) {
            throw new IllegalArgumentException(
                    "Slot generation requires duration and max patients. " +
                    "Configure clinic settings or provide values in the request.");
        }

        List<AppointmentSlot> generatedSlots = new ArrayList<>();

        for (String dayStr : request.getDaysOfWeek()) {
            DayOfWeekEnum day = parseDayOfWeek(dayStr);

            // Try to get per-day schedule first, fall back to global settings or request values
            Optional<DaySchedule> dayScheduleOpt = dayScheduleRepository.findByDayOfWeek(day);

            LocalTime startTime;
            LocalTime endTime;
            List<DayScheduleBreak> breaks = new ArrayList<>();

            if (dayScheduleOpt.isPresent()) {
                DaySchedule ds = dayScheduleOpt.get();
                if (!ds.isWorkingDay()) {
                    continue; // Skip non-working days
                }
                startTime = ds.getStartTime();
                endTime = ds.getEndTime();
                if (ds.getBreaks() != null) {
                    breaks = ds.getBreaks();
                }
            } else {
                // Fall back to request values or clinic settings
                String startTimeStr = request.getStartTime();
                String endTimeStr = request.getEndTime();
                if (settings != null) {
                    if (startTimeStr == null) startTimeStr = settings.getWorkingStartTime().format(TIME_FMT);
                    if (endTimeStr == null) endTimeStr = settings.getWorkingEndTime().format(TIME_FMT);
                }
                if (startTimeStr == null || endTimeStr == null) {
                    throw new IllegalArgumentException(
                            "No day schedule found for " + day + " and no fallback start/end time available.");
                }
                startTime = LocalTime.parse(startTimeStr, TIME_FMT);
                endTime = LocalTime.parse(endTimeStr, TIME_FMT);
                // Use single break from clinic settings as fallback
                if (settings != null && settings.getBreakStartTime() != null && settings.getBreakEndTime() != null) {
                    DayScheduleBreak fallbackBreak = new DayScheduleBreak();
                    fallbackBreak.setBreakStart(settings.getBreakStartTime());
                    fallbackBreak.setBreakEnd(settings.getBreakEndTime());
                    breaks.add(fallbackBreak);
                }
            }

            LocalTime currentTime = startTime;

            while (currentTime.plusMinutes(duration).compareTo(endTime) <= 0) {
                LocalTime slotEnd = currentTime.plusMinutes(duration);

                // Check against all break periods
                boolean inBreak = false;
                LocalTime skipTo = null;
                for (DayScheduleBreak brk : breaks) {
                    LocalTime bStart = brk.getBreakStart();
                    LocalTime bEnd = brk.getBreakEnd();
                    // Current time is within break
                    if (!currentTime.isBefore(bStart) && currentTime.isBefore(bEnd)) {
                        inBreak = true;
                        skipTo = bEnd;
                        break;
                    }
                    // Slot would cross into break
                    if (currentTime.isBefore(bStart) && slotEnd.isAfter(bStart)) {
                        inBreak = true;
                        skipTo = bEnd;
                        break;
                    }
                }

                if (inBreak && skipTo != null) {
                    currentTime = skipTo;
                    continue;
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

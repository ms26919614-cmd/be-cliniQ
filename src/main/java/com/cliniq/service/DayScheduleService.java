package com.cliniq.service;

import com.cliniq.dto.DayScheduleRequest;
import com.cliniq.dto.DayScheduleResponse;
import com.cliniq.entity.DaySchedule;
import com.cliniq.entity.DayScheduleBreak;
import com.cliniq.enums.DayOfWeekEnum;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.DayScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DayScheduleService {

    private final DayScheduleRepository dayScheduleRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<DayScheduleResponse> getAllSchedules() {
        return dayScheduleRepository.findAllByOrderByDayOfWeekAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DayScheduleResponse getScheduleByDay(String dayOfWeek) {
        DayOfWeekEnum day = parseDayOfWeek(dayOfWeek);
        DaySchedule schedule = dayScheduleRepository.findByDayOfWeek(day)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found for " + dayOfWeek));
        return mapToResponse(schedule);
    }

    @Transactional
    public DayScheduleResponse updateSchedule(String dayOfWeek, DayScheduleRequest request) {
        DayOfWeekEnum day = parseDayOfWeek(dayOfWeek);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FMT);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FMT);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        DaySchedule schedule = dayScheduleRepository.findByDayOfWeek(day)
                .orElse(DaySchedule.builder()
                        .dayOfWeek(day)
                        .breaks(new ArrayList<>())
                        .build());

        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setWorkingDay(request.getWorkingDay());

        // Clear existing breaks and replace
        schedule.getBreaks().clear();

        if (request.getBreaks() != null) {
            for (DayScheduleRequest.BreakRequest br : request.getBreaks()) {
                LocalTime breakStart = LocalTime.parse(br.getBreakStart(), TIME_FMT);
                LocalTime breakEnd = LocalTime.parse(br.getBreakEnd(), TIME_FMT);

                if (!breakEnd.isAfter(breakStart)) {
                    throw new IllegalArgumentException("Break end time must be after break start time");
                }
                if (breakStart.isBefore(startTime) || breakEnd.isAfter(endTime)) {
                    throw new IllegalArgumentException("Break time must be within working hours");
                }

                DayScheduleBreak breakEntity = DayScheduleBreak.builder()
                        .daySchedule(schedule)
                        .breakStart(breakStart)
                        .breakEnd(breakEnd)
                        .label(br.getLabel() != null ? br.getLabel() : "Break")
                        .build();
                schedule.getBreaks().add(breakEntity);
            }
        }

        DaySchedule saved = dayScheduleRepository.save(schedule);
        return mapToResponse(saved);
    }

    @Transactional
    public List<DayScheduleResponse> bulkUpdateSchedules(List<DayScheduleRequest> requests) {
        List<DayScheduleResponse> responses = new ArrayList<>();
        for (DayScheduleRequest request : requests) {
            responses.add(updateSchedule(request.getDayOfWeek(), request));
        }
        return responses;
    }

    private DayOfWeekEnum parseDayOfWeek(String day) {
        try {
            return DayOfWeekEnum.valueOf(day.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid day of week: '" + day + "'. Must be one of: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
        }
    }

    private DayScheduleResponse mapToResponse(DaySchedule schedule) {
        List<DayScheduleResponse.BreakResponse> breakResponses = schedule.getBreaks() != null
                ? schedule.getBreaks().stream()
                    .map(b -> DayScheduleResponse.BreakResponse.builder()
                            .id(b.getId())
                            .breakStart(b.getBreakStart().format(TIME_FMT))
                            .breakEnd(b.getBreakEnd().format(TIME_FMT))
                            .label(b.getLabel())
                            .build())
                    .collect(Collectors.toList())
                : new ArrayList<>();

        return DayScheduleResponse.builder()
                .id(schedule.getId())
                .dayOfWeek(schedule.getDayOfWeek().name())
                .startTime(schedule.getStartTime().format(TIME_FMT))
                .endTime(schedule.getEndTime().format(TIME_FMT))
                .workingDay(schedule.isWorkingDay())
                .breaks(breakResponses)
                .createdAt(schedule.getCreatedAt() != null ? schedule.getCreatedAt().format(DATETIME_FMT) : null)
                .updatedAt(schedule.getUpdatedAt() != null ? schedule.getUpdatedAt().format(DATETIME_FMT) : null)
                .build();
    }
}

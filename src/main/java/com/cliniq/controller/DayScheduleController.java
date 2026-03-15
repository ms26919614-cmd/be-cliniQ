package com.cliniq.controller;

import com.cliniq.dto.DayScheduleRequest;
import com.cliniq.dto.DayScheduleResponse;
import com.cliniq.service.DayScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day-schedules")
@RequiredArgsConstructor
public class DayScheduleController {

    private final DayScheduleService dayScheduleService;

    @GetMapping
    public ResponseEntity<List<DayScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(dayScheduleService.getAllSchedules());
    }

    @GetMapping("/{dayOfWeek}")
    public ResponseEntity<DayScheduleResponse> getScheduleByDay(@PathVariable String dayOfWeek) {
        return ResponseEntity.ok(dayScheduleService.getScheduleByDay(dayOfWeek));
    }

    @PutMapping("/{dayOfWeek}")
    public ResponseEntity<DayScheduleResponse> updateSchedule(
            @PathVariable String dayOfWeek,
            @Valid @RequestBody DayScheduleRequest request) {
        return ResponseEntity.ok(dayScheduleService.updateSchedule(dayOfWeek, request));
    }

    @PutMapping("/bulk")
    public ResponseEntity<List<DayScheduleResponse>> bulkUpdateSchedules(
            @Valid @RequestBody List<DayScheduleRequest> requests) {
        return ResponseEntity.ok(dayScheduleService.bulkUpdateSchedules(requests));
    }
}

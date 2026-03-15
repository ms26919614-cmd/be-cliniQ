package com.cliniq.controller;

import com.cliniq.dto.*;
import com.cliniq.service.AppointmentSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointment-slots")
@RequiredArgsConstructor
public class AppointmentSlotController {

    private final AppointmentSlotService appointmentSlotService;

    @GetMapping
    public ResponseEntity<List<AppointmentSlotResponse>> getAllSlots() {
        return ResponseEntity.ok(appointmentSlotService.getAllSlots());
    }

    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List<AppointmentSlotResponse>> getSlotsByDay(
            @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(appointmentSlotService.getSlotsByDay(dayOfWeek));
    }

    @GetMapping("/available")
    public ResponseEntity<List<AppointmentSlotResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentSlotService.getAvailableSlotsForDate(date));
    }

    @PostMapping
    public ResponseEntity<AppointmentSlotResponse> createSlot(
            @Valid @RequestBody AppointmentSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentSlotService.createSlot(request));
    }

    @PutMapping("/{slotId}")
    public ResponseEntity<AppointmentSlotResponse> updateSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody AppointmentSlotRequest request) {
        return ResponseEntity.ok(appointmentSlotService.updateSlot(slotId, request));
    }

    @PatchMapping("/{slotId}/toggle")
    public ResponseEntity<AppointmentSlotResponse> toggleSlotActive(
            @PathVariable Long slotId) {
        return ResponseEntity.ok(appointmentSlotService.toggleSlotActive(slotId));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Map<String, String>> deleteSlot(@PathVariable Long slotId) {
        appointmentSlotService.deleteSlot(slotId);
        return ResponseEntity.ok(Map.of("message", "Slot deleted successfully"));
    }

    @PostMapping("/generate")
    public ResponseEntity<List<AppointmentSlotResponse>> generateSlots(
            @Valid @RequestBody GenerateSlotsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentSlotService.generateSlots(request));
    }
}

package com.cliniq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class VisitResponse {

    private Long id;
    private Integer tokenNumber;
    private String patientName;
    private String phone;
    private LocalDate visitDate;
    private String status;
    private String visitType;        // WALK_IN or APPOINTMENT
    private String appointmentTime;  // HH:mm if appointment, null for walk-ins
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
}

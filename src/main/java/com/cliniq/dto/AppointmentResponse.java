package com.cliniq.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private String phone;
    private Long slotId;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private Integer tokenNumber;
    private String status;
    private String notes;
    private String createdAt;
    private String updatedAt;
    private String cancelledAt;
}

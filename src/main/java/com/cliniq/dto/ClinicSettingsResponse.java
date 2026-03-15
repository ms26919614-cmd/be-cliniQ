package com.cliniq.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSettingsResponse {

    private Long id;
    private String clinicName;
    private String workingStartTime;
    private String workingEndTime;
    private Integer slotDurationMinutes;
    private Integer maxPatientsPerSlot;
    private String breakStartTime;
    private String breakEndTime;
    private String updatedAt;
}

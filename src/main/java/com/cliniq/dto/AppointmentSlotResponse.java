package com.cliniq.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSlotResponse {

    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private Integer maxPatients;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}

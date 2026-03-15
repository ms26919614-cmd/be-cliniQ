package com.cliniq.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSlotRequest {

    @NotNull(message = "Day of week is required")
    private String dayOfWeek;

    @NotNull(message = "Start time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Start time must be in HH:mm format")
    private String startTime;

    @NotNull(message = "End time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "End time must be in HH:mm format")
    private String endTime;

    @NotNull(message = "Max patients is required")
    @Min(value = 1, message = "At least 1 patient per slot")
    @Max(value = 20, message = "Maximum 20 patients per slot")
    private Integer maxPatients;
}

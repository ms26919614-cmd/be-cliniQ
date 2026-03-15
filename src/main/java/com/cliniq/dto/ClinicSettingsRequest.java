package com.cliniq.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSettingsRequest {

    @NotBlank(message = "Clinic name is required")
    private String clinicName;

    @NotNull(message = "Working start time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Start time must be in HH:mm format")
    private String workingStartTime;

    @NotNull(message = "Working end time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "End time must be in HH:mm format")
    private String workingEndTime;

    @NotNull(message = "Slot duration is required")
    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 120, message = "Slot duration cannot exceed 120 minutes")
    private Integer slotDurationMinutes;

    @NotNull(message = "Max patients per slot is required")
    @Min(value = 1, message = "At least 1 patient per slot")
    @Max(value = 10, message = "Maximum 10 patients per slot")
    private Integer maxPatientsPerSlot;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Break start time must be in HH:mm format")
    private String breakStartTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Break end time must be in HH:mm format")
    private String breakEndTime;
}

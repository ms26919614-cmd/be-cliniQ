package com.cliniq.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateSlotsRequest {

    @NotNull(message = "Days of week are required")
    @Size(min = 1, message = "At least one day must be selected")
    private List<String> daysOfWeek;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Start time must be in HH:mm format")
    private String startTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "End time must be in HH:mm format")
    private String endTime;

    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 120, message = "Slot duration cannot exceed 120 minutes")
    private Integer slotDurationMinutes;

    @Min(value = 1, message = "At least 1 patient per slot")
    @Max(value = 20, message = "Maximum 20 patients per slot")
    private Integer maxPatientsPerSlot;
}

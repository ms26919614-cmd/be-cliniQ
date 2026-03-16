package com.cliniq.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayScheduleRequest {

    @NotBlank(message = "Day of week is required")
    private String dayOfWeek;

    @NotNull(message = "Start time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Start time must be in HH:mm format")
    private String startTime;

    @NotNull(message = "End time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "End time must be in HH:mm format")
    private String endTime;

    @NotNull(message = "Working day flag is required")
    private Boolean workingDay;

    private List<BreakRequest> breaks;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreakRequest {
        @NotNull(message = "Break start time is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Break start must be in HH:mm format")
        private String breakStart;

        @NotNull(message = "Break end time is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Break end must be in HH:mm format")
        private String breakEnd;

        private String label;
    }
}

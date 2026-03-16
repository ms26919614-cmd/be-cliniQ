package com.cliniq.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayScheduleResponse {

    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private boolean workingDay;
    private List<BreakResponse> breaks;
    private String createdAt;
    private String updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreakResponse {
        private Long id;
        private String breakStart;
        private String breakEnd;
        private String label;
    }
}

package com.cliniq.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayOverrideResponse {

    private Long id;
    private String overrideDate;
    private boolean workingDay;
    private String startTime;
    private String endTime;
    private String reason;
    private String createdAt;
    private String updatedAt;
}

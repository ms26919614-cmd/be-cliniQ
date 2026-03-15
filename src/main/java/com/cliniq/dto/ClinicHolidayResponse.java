package com.cliniq.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicHolidayResponse {

    private Long id;
    private String holidayDate;
    private String description;
    private String createdAt;
}

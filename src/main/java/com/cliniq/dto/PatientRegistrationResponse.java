package com.cliniq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class PatientRegistrationResponse {

    private Long visitId;
    private Integer tokenNumber;
    private String patientName;
    private String phone;
    private LocalDate visitDate;
    private String status;
    private String message;
}

package com.cliniq.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentBookingRequest {

    @NotBlank(message = "Patient name is required")
    private String patientName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    private String phone;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "Appointment date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in yyyy-MM-dd format")
    private String appointmentDate;

    private String notes;
}

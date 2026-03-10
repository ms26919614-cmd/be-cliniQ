package com.cliniq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(IN_PROGRESS|COMPLETED|NO_SHOW)$",
            message = "Status must be one of: IN_PROGRESS, COMPLETED, NO_SHOW")
    private String status;
}

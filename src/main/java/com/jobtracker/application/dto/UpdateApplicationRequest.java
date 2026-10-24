package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateApplicationRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 120, message = "Company name must not exceed 120 characters")
        String company,

        @NotBlank(message = "Position title is required")
        @Size(max = 120, message = "Position title must not exceed 120 characters")
        String position,

        @NotNull(message = "Application status is required")
        ApplicationStatus status,

        @Size(max = 500, message = "Job link must not exceed 500 characters")
        String link,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        LocalDate appliedAt
) {
}

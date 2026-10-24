package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Application status is required")
        ApplicationStatus status
) {
}

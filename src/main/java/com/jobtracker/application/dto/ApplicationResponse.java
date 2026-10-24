package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ApplicationResponse(
        Long id,
        String company,
        String position,
        ApplicationStatus status,
        String link,
        String notes,
        LocalDate appliedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

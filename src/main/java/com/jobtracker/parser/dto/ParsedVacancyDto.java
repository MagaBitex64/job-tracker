package com.jobtracker.parser.dto;

import java.time.LocalDate;

public record ParsedVacancyDto(
        String company,
        String position,
        String link,
        String notes,
        LocalDate appliedAt,
        String source // "HH" or "LINKEDIN"
) {
}

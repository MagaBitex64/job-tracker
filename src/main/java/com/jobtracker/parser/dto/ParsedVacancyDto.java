package com.jobtracker.parser.dto;

import java.time.LocalDate;

public record ParsedVacancyDto(
        String company,
        String position,
        String link,
        String notes,
        LocalDate appliedAt,
        String source, // "HH" or "LINKEDIN"
        String location,
        String salary,
        String experience
) {
    // Backwards-compatible constructor for existing tests and call sites
    public ParsedVacancyDto(String company, String position, String link, String notes, LocalDate appliedAt, String source) {
        this(company, position, link, notes, appliedAt, source, null, null, null);
    }
}

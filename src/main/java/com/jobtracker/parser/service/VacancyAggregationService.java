package com.jobtracker.parser.service;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.parser.dto.ImportVacancyRequest;
import com.jobtracker.parser.dto.ParsedVacancyDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class VacancyAggregationService {

    private final HeadHunterParserService headHunterParserService;
    private final LinkedInParserService linkedInParserService;
    private final ApplicationService applicationService;

    public VacancyAggregationService(
            HeadHunterParserService headHunterParserService,
            LinkedInParserService linkedInParserService,
            ApplicationService applicationService
    ) {
        this.headHunterParserService = headHunterParserService;
        this.linkedInParserService = linkedInParserService;
        this.applicationService = applicationService;
    }

    public List<ParsedVacancyDto> searchAcrossPlatforms(String keyword, String source, int limit) {
        List<ParsedVacancyDto> combined = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return combined;
        }

        String src = source != null ? source.toUpperCase().trim() : "ALL";

        if ("ALL".equals(src) || "HH".equals(src)) {
            combined.addAll(headHunterParserService.searchVacancies(keyword, limit));
        }

        if ("ALL".equals(src) || "LINKEDIN".equals(src)) {
            combined.addAll(linkedInParserService.searchVacancies(keyword, limit));
        }

        return combined;
    }

    @Transactional
    public ApplicationResponse importVacancy(Long userId, ImportVacancyRequest request) {
        CreateApplicationRequest createRequest = new CreateApplicationRequest(
                request.company(),
                request.position(),
                request.status() != null ? request.status() : ApplicationStatus.WISHLIST,
                request.link(),
                request.notes(),
                LocalDate.now()
        );
        return applicationService.createApplication(userId, createRequest);
    }
}

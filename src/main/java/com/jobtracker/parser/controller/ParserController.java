package com.jobtracker.parser.controller;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.parser.dto.ImportVacancyRequest;
import com.jobtracker.parser.dto.ParsedVacancyDto;
import com.jobtracker.parser.service.VacancyAggregationService;
import com.jobtracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parser")
@Tag(name = "Vacancy Parser", description = "Endpoints for searching and parsing vacancies from HeadHunter and LinkedIn")
public class ParserController {

    private final VacancyAggregationService vacancyAggregationService;

    public ParserController(VacancyAggregationService vacancyAggregationService) {
        this.vacancyAggregationService = vacancyAggregationService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search vacancies from HeadHunter and LinkedIn by keyword")
    public ResponseEntity<List<ParsedVacancyDto>> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "ALL") String source,
            @RequestParam(required = false, defaultValue = "15") int limit
    ) {
        List<ParsedVacancyDto> vacancies = vacancyAggregationService.searchAcrossPlatforms(query, source, limit);
        return ResponseEntity.ok(vacancies);
    }

    @PostMapping("/import")
    @Operation(summary = "Import a parsed vacancy into user's Kanban board")
    public ResponseEntity<ApplicationResponse> importVacancy(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ImportVacancyRequest request
    ) {
        ApplicationResponse response = vacancyAggregationService.importVacancy(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

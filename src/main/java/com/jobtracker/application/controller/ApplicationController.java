package com.jobtracker.application.controller;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.ApplicationStatsResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.dto.UpdateStatusRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications", description = "Endpoints for managing user job applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new job application")
    public ResponseEntity<ApplicationResponse> createApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.createApplication(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job application by ID")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        ApplicationResponse response = applicationService.getApplicationById(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List job applications with pagination, sorting, status filter, and search")
    public ResponseEntity<Page<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20)
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ApplicationResponse> response = applicationService.getApplications(
                principal.getId(), status, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing job application")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.updateApplication(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of a job application (e.g. Kanban column move)")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        ApplicationResponse response = applicationService.updateApplicationStatus(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a job application by ID")
    public void deleteApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        applicationService.deleteApplication(principal.getId(), id);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get application count statistics per status for the current user")
    public ResponseEntity<ApplicationStatsResponse> getApplicationStats(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ApplicationStatsResponse response = applicationService.getApplicationStats(principal.getId());
        return ResponseEntity.ok(response);
    }
}

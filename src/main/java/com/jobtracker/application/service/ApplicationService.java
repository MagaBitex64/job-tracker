package com.jobtracker.application.service;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.ApplicationStatsResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.dto.UpdateStatusRequest;
import com.jobtracker.application.entity.Application;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.mapper.ApplicationMapper;
import com.jobtracker.application.repository.ApplicationRepository;
import com.jobtracker.application.repository.StatusCountProjection;
import com.jobtracker.common.exception.ResourceNotFoundException;
import com.jobtracker.user.entity.User;
import com.jobtracker.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            ApplicationMapper applicationMapper
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationMapper = applicationMapper;
    }

    @Transactional
    public ApplicationResponse createApplication(Long userId, CreateApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Application application = applicationMapper.toEntity(request, user);
        Application savedApplication = applicationRepository.save(application);

        return applicationMapper.toResponse(savedApplication);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long userId, Long applicationId) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId
                ));

        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplications(
            Long userId,
            ApplicationStatus status,
            String search,
            Pageable pageable
    ) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Application> page = applicationRepository.findAllByUserAndFilters(userId, status, trimmedSearch, pageable);
        return page.map(applicationMapper::toResponse);
    }

    @Transactional
    public ApplicationResponse updateApplication(Long userId, Long applicationId, UpdateApplicationRequest request) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId
                ));

        applicationMapper.updateEntity(application, request);
        Application updatedApplication = applicationRepository.save(application);

        return applicationMapper.toResponse(updatedApplication);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long userId, Long applicationId, UpdateStatusRequest request) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId
                ));

        application.setStatus(request.status());
        Application updatedApplication = applicationRepository.save(application);

        return applicationMapper.toResponse(updatedApplication);
    }

    @Transactional
    public void deleteApplication(Long userId, Long applicationId) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId
                ));

        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    public ApplicationStatsResponse getApplicationStats(Long userId) {
        List<StatusCountProjection> projections = applicationRepository.countApplicationsByStatusForUser(userId);

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }

        long total = 0;
        for (StatusCountProjection projection : projections) {
            long count = projection.getCount();
            counts.put(projection.getStatus(), count);
            total += count;
        }

        return new ApplicationStatsResponse(
                counts.get(ApplicationStatus.WISHLIST),
                counts.get(ApplicationStatus.APPLIED),
                counts.get(ApplicationStatus.INTERVIEW),
                counts.get(ApplicationStatus.OFFER),
                counts.get(ApplicationStatus.REJECTED),
                total
        );
    }
}

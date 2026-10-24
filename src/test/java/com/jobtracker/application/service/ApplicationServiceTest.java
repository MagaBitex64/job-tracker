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
import com.jobtracker.user.entity.Role;
import com.jobtracker.user.entity.User;
import com.jobtracker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ApplicationMapper applicationMapper = new ApplicationMapper();

    @InjectMocks
    private ApplicationService applicationService;

    private User user;
    private Application application;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .role(Role.ROLE_USER)
                .build();

        application = Application.builder()
                .id(100L)
                .user(user)
                .company("Acme Corp")
                .position("Senior Backend Engineer")
                .status(ApplicationStatus.APPLIED)
                .link("https://example.com/jobs/1")
                .notes("Referral via Alice")
                .appliedAt(LocalDate.of(2026, 10, 15))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createApplication should save and return created application")
    void shouldCreateApplicationSuccessfully() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "Acme Corp", "Senior Backend Engineer", ApplicationStatus.APPLIED,
                "https://example.com/jobs/1", "Referral via Alice", LocalDate.of(2026, 10, 15)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        ApplicationResponse response = applicationService.createApplication(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.company()).isEqualTo("Acme Corp");
        assertThat(response.position()).isEqualTo("Senior Backend Engineer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);

        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    @DisplayName("createApplication should throw ResourceNotFoundException when user does not exist")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "Acme Corp", "Backend Engineer", ApplicationStatus.WISHLIST, null, null, null
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("getApplicationById should return application when owned by user")
    void shouldReturnApplicationWhenOwnedByUser() {
        when(applicationRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getApplicationById(1L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.company()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("getApplicationById should throw ResourceNotFoundException when application belongs to another user")
    void shouldThrowResourceNotFoundExceptionWhenApplicationBelongsToAnotherUser() {
        // Enforces ownership isolation: user 2 attempting to view user 1's application returns 404
        when(applicationRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(2L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: 100");
    }

    @Test
    @DisplayName("getApplications should return paginated applications with search and filter")
    void shouldReturnPaginatedApplicationsWithFilterAndSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> page = new PageImpl<>(List.of(application), pageable, 1);

        when(applicationRepository.findAllByUserAndFilters(1L, ApplicationStatus.APPLIED, "Acme", pageable))
                .thenReturn(page);

        Page<ApplicationResponse> response = applicationService.getApplications(
                1L, ApplicationStatus.APPLIED, " Acme ", pageable
        );

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).company()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("updateApplication should modify fields and save when owned by user")
    void shouldUpdateApplicationSuccessfully() {
        UpdateApplicationRequest updateRequest = new UpdateApplicationRequest(
                "Acme Corp Updated", "Lead Engineer", ApplicationStatus.INTERVIEW,
                "https://example.com/jobs/lead", "Second round scheduled", LocalDate.of(2026, 10, 16)
        );

        when(applicationRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = applicationService.updateApplication(1L, 100L, updateRequest);

        assertThat(response.company()).isEqualTo("Acme Corp Updated");
        assertThat(response.position()).isEqualTo("Lead Engineer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(response.notes()).isEqualTo("Second round scheduled");
    }

    @Test
    @DisplayName("updateApplication should throw ResourceNotFoundException when owned by another user")
    void shouldThrowResourceNotFoundExceptionWhenUpdatingUnownedApplication() {
        UpdateApplicationRequest updateRequest = new UpdateApplicationRequest(
                "Malicious Corp", "Hacker", ApplicationStatus.OFFER, null, null, null
        );

        when(applicationRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplication(2L, 100L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: 100");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("updateApplicationStatus should update status when owned by user")
    void shouldUpdateApplicationStatusSuccessfully() {
        UpdateStatusRequest statusRequest = new UpdateStatusRequest(ApplicationStatus.OFFER);

        when(applicationRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, 100L, statusRequest);

        assertThat(response.status()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    @DisplayName("updateApplicationStatus should throw ResourceNotFoundException when owned by another user")
    void shouldThrowResourceNotFoundExceptionWhenUpdatingStatusOfUnownedApplication() {
        UpdateStatusRequest statusRequest = new UpdateStatusRequest(ApplicationStatus.OFFER);

        when(applicationRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(2L, 100L, statusRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: 100");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("deleteApplication should remove entity when owned by user")
    void shouldDeleteApplicationSuccessfully() {
        when(applicationRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(1L, 100L);

        verify(applicationRepository).delete(application);
    }

    @Test
    @DisplayName("deleteApplication should throw ResourceNotFoundException when owned by another user")
    void shouldThrowResourceNotFoundExceptionWhenDeletingUnownedApplication() {
        when(applicationRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(2L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: 100");

        verify(applicationRepository, never()).delete(any(Application.class));
    }

    @Test
    @DisplayName("getApplicationStats should calculate status counts correctly")
    void shouldCalculateStatusStatsCorrectly() {
        StatusCountProjection p1 = new StatusCountProjection() {
            @Override
            public ApplicationStatus getStatus() {
                return ApplicationStatus.APPLIED;
            }
            @Override
            public long getCount() {
                return 4;
            }
        };
        StatusCountProjection p2 = new StatusCountProjection() {
            @Override
            public ApplicationStatus getStatus() {
                return ApplicationStatus.INTERVIEW;
            }
            @Override
            public long getCount() {
                return 2;
            }
        };

        when(applicationRepository.countApplicationsByStatusForUser(1L)).thenReturn(List.of(p1, p2));

        ApplicationStatsResponse stats = applicationService.getApplicationStats(1L);

        assertThat(stats.applied()).isEqualTo(4);
        assertThat(stats.interview()).isEqualTo(2);
        assertThat(stats.wishlist()).isEqualTo(0);
        assertThat(stats.offer()).isEqualTo(0);
        assertThat(stats.rejected()).isEqualTo(0);
        assertThat(stats.total()).isEqualTo(6);
    }
}

package com.jobtracker.application.mapper;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.entity.Application;
import com.jobtracker.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        if (application == null) {
            return null;
        }
        return new ApplicationResponse(
                application.getId(),
                application.getCompany(),
                application.getPosition(),
                application.getStatus(),
                application.getLink(),
                application.getNotes(),
                application.getAppliedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    public Application toEntity(CreateApplicationRequest request, User user) {
        if (request == null) {
            return null;
        }
        return Application.builder()
                .user(user)
                .company(request.company().trim())
                .position(request.position().trim())
                .status(request.status())
                .link(request.link() != null ? request.link().trim() : null)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .appliedAt(request.appliedAt())
                .build();
    }

    public void updateEntity(Application application, UpdateApplicationRequest request) {
        application.setCompany(request.company().trim());
        application.setPosition(request.position().trim());
        application.setStatus(request.status());
        application.setLink(request.link() != null ? request.link().trim() : null);
        application.setNotes(request.notes() != null ? request.notes().trim() : null);
        application.setAppliedAt(request.appliedAt());
    }
}

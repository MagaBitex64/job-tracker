package com.jobtracker.user.dto;

import com.jobtracker.user.entity.Role;

public record UserSummaryResponse(
        Long id,
        String email,
        String fullName,
        Role role
) {
}

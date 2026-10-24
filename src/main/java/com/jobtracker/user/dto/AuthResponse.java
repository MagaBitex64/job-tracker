package com.jobtracker.user.dto;

public record AuthResponse(
        String token,
        String type,
        UserSummaryResponse user
) {
    public static AuthResponse of(String token, UserSummaryResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}

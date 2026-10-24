package com.jobtracker.application.dto;

public record ApplicationStatsResponse(
        long wishlist,
        long applied,
        long interview,
        long offer,
        long rejected,
        long total
) {
}

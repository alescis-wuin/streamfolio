package dev.sey.streamfolio.catalog.dto;

import jakarta.validation.constraints.Min;

public record ProgressUpdateRequest(
    @Min(0) int positionSeconds,
    @Min(1) int durationSeconds
) {
}

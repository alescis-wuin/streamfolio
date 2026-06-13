package dev.sey.streamfolio.catalog.dto;

import dev.sey.streamfolio.domain.UserProgress;
import java.time.Instant;

public record ProgressDto(
    int positionSeconds,
    int durationSeconds,
    double percent,
    boolean completed,
    Instant updatedAt
) {
    public static ProgressDto empty(int durationSeconds) {
        return new ProgressDto(0, Math.max(1, durationSeconds), 0.0, false, null);
    }

    public static ProgressDto from(UserProgress progress) {
        int duration = Math.max(1, progress.getDurationSeconds());
        double percent = Math.min(100.0, Math.round((progress.getPositionSeconds() * 10000.0 / duration)) / 100.0);
        return new ProgressDto(
            progress.getPositionSeconds(),
            duration,
            percent,
            progress.isCompleted(),
            progress.getUpdatedAt()
        );
    }

    public static ProgressDto aggregate(int positionSeconds, int durationSeconds, Instant updatedAt) {
        int duration = Math.max(1, durationSeconds);
        int position = Math.max(0, Math.min(positionSeconds, duration));
        double percent = Math.min(100.0, Math.round((position * 10000.0 / duration)) / 100.0);
        return new ProgressDto(position, duration, percent, percent >= 90.0, updatedAt);
    }
}

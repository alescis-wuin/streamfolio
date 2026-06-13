package dev.sey.streamfolio.catalog.dto;

public record VideoDto(
    Long id,
    int seasonNumber,
    int episodeNumber,
    String label,
    String title,
    int durationSeconds,
    String streamUrl,
    String subtitlesUrl,
    ProgressDto progress
) {
}

package dev.sey.streamfolio.admin;

import java.util.List;

public record AdminVideoUpdateRequest(
    String title,
    Integer releaseYear,
    String maturityRating,
    Integer runtimeMinutes,
    String tagline,
    String synopsis,
    List<String> genres,
    String label,
    String videoTitle,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer durationSeconds,
    String publicationStatus
) {
}

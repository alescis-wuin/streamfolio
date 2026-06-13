package dev.sey.streamfolio.catalog.dto;

import dev.sey.streamfolio.domain.ContentType;
import java.util.List;

public record PlaybackDto(
    Long videoId,
    String titleSlug,
    String title,
    ContentType type,
    String videoTitle,
    String label,
    int seasonNumber,
    int episodeNumber,
    int releaseYear,
    String maturityRating,
    List<String> genres,
    String streamUrl,
    String hlsUrl,
    StreamingMode streamingMode,
    String subtitlesUrl,
    ProgressDto progress
) {
}

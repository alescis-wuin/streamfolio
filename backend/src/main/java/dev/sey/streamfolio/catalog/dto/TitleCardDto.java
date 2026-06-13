package dev.sey.streamfolio.catalog.dto;

import dev.sey.streamfolio.domain.ContentType;
import java.util.List;

public record TitleCardDto(
    Long id,
    String slug,
    String title,
    ContentType type,
    int releaseYear,
    String maturityRating,
    int runtimeMinutes,
    int seasonCount,
    int episodeCount,
    String tagline,
    String synopsis,
    List<String> genres,
    String posterPath,
    String backdropPath,
    boolean inWatchlist,
    ProgressDto progress,
    Long nextVideoId
) {
}

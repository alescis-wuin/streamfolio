package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.MediaAssetStatus;
import java.util.List;

public record AdminVideoDto(
    Long videoId,
    Long titleId,
    String slug,
    String title,
    ContentType type,
    int releaseYear,
    String maturityRating,
    int runtimeMinutes,
    String tagline,
    String synopsis,
    List<String> genres,
    String posterPath,
    String backdropPath,
    String label,
    String videoTitle,
    int seasonNumber,
    int episodeNumber,
    int durationSeconds,
    String publicationStatus,
    String assetFilename,
    String subtitleFilename,
    MediaAssetStatus assetStatus,
    String contentSha256,
    String contentType,
    long sizeBytes
) {
    public static AdminVideoDto from(CatalogVideo video, MediaAsset asset) {
        return new AdminVideoDto(
            video.getId(),
            video.getTitle().getId(),
            video.getTitle().getSlug(),
            video.getTitle().getTitle(),
            video.getTitle().getType(),
            video.getTitle().getReleaseYear(),
            video.getTitle().getMaturityRating(),
            video.getTitle().getRuntimeMinutes(),
            video.getTitle().getTagline(),
            video.getTitle().getSynopsis(),
            List.copyOf(video.getTitle().getGenres()),
            video.getTitle().getPosterPath(),
            video.getTitle().getBackdropPath(),
            video.getLabel(),
            video.getVideoTitle(),
            video.getSeasonNumber(),
            video.getEpisodeNumber(),
            video.getDurationSeconds(),
            video.getPublicationStatus(),
            video.getAssetFilename(),
            video.getSubtitleFilename(),
            asset == null ? null : asset.getStatus(),
            asset == null ? null : asset.getContentSha256(),
            asset == null ? null : asset.getContentType(),
            asset == null ? 0 : asset.getSizeBytes()
        );
    }
}

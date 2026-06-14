package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.MediaAssetStatus;
import java.time.Instant;

public record MediaAssetDto(
    Long id,
    Long videoId,
    String title,
    String videoTitle,
    String originalFilename,
    String hlsMasterPath,
    String thumbnailManifestPath,
    MediaAssetStatus status,
    Instant updatedAt
) {
    public static MediaAssetDto from(MediaAsset asset) {
        return new MediaAssetDto(
            asset.getId(),
            asset.getVideo().getId(),
            asset.getVideo().getTitle().getTitle(),
            asset.getVideo().getVideoTitle(),
            asset.getOriginalFilename(),
            asset.getHlsMasterPath(),
            asset.getThumbnailManifestPath(),
            asset.getStatus(),
            asset.getUpdatedAt()
        );
    }
}

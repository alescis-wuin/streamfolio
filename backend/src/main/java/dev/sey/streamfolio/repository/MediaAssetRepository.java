package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.MediaAsset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    @EntityGraph(attributePaths = {"video", "video.title"})
    Optional<MediaAsset> findByVideo(CatalogVideo video);

    void deleteByVideo(CatalogVideo video);

    @Override
    @EntityGraph(attributePaths = {"video", "video.title"})
    List<MediaAsset> findAll();

    @EntityGraph(attributePaths = {"video", "video.title"})
    List<MediaAsset> findByVideoIdIn(Collection<Long> videoIds);

    Optional<MediaAsset> findFirstByContentSha256(String contentSha256);
}

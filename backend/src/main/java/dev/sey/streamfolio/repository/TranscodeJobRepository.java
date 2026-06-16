package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.TranscodeJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, Long> {
    @EntityGraph(attributePaths = {"video", "video.title"})
    List<TranscodeJob> findTop25ByOrderByRequestedAtDesc();

    @EntityGraph(attributePaths = {"video", "video.title"})
    @Query("select job from TranscodeJob job where job.id = :id")
    Optional<TranscodeJob> findWithVideoById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"video", "video.title"})
    List<TranscodeJob> findByParentJobIdOrderByRequestedAtAscIdAsc(Long parentJobId);

    void deleteByVideo(CatalogVideo video);
}

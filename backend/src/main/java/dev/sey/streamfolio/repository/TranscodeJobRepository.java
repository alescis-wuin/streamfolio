package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.TranscodeJob;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, Long> {
    @EntityGraph(attributePaths = {"video", "video.title"})
    List<TranscodeJob> findTop25ByOrderByRequestedAtDesc();
}

package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = {"video", "video.title"})
    List<TranscodeJob> findByStatusIn(Collection<TranscodeJobStatus> statuses);

    @EntityGraph(attributePaths = {"video", "video.title"})
    @Query("""
        select job
        from TranscodeJob job
        where job.video = :video
          and job.workItem = :workItem
          and job.status in :statuses
        order by job.requestedAt desc, job.id desc
        """)
    List<TranscodeJob> findActiveByVideoAndWorkItem(@Param("video") CatalogVideo video,
                                                    @Param("workItem") String workItem,
                                                    @Param("statuses") Collection<TranscodeJobStatus> statuses,
                                                    Pageable pageable);

    @EntityGraph(attributePaths = {"video", "video.title"})
    @Query("""
        select job
        from TranscodeJob job
        where job.status in :statuses
          and job.workItem <> 'batch'
          and job.cancellationRequested = false
          and (job.nextAttemptAt is null or job.nextAttemptAt <= :now)
        order by job.requestedAt asc, job.id asc
        """)
    List<TranscodeJob> findRunnableJobs(@Param("statuses") Collection<TranscodeJobStatus> statuses,
                                        @Param("now") Instant now,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"video", "video.title"})
    @Query("""
        select job
        from TranscodeJob job
        where job.status = :status
          and job.workItem <> 'batch'
          and (job.lastHeartbeatAt is null or job.lastHeartbeatAt < :cutoff)
        order by job.requestedAt asc, job.id asc
        """)
    List<TranscodeJob> findStaleRunningJobs(@Param("status") TranscodeJobStatus status,
                                            @Param("cutoff") Instant cutoff);

    void deleteByVideo(CatalogVideo video);
}

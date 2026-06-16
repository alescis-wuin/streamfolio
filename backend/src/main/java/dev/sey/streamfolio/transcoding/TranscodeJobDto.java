package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import java.time.Instant;

public record TranscodeJobDto(
    Long id,
    Long videoId,
    String title,
    String videoTitle,
    TranscodeJobStatus status,
    int progressPercent,
    boolean force,
    String workItem,
    Long parentJobId,
    String workerName,
    Instant requestedAt,
    Instant startedAt,
    Instant finishedAt,
    String message,
    String outputPath
) {
    public static TranscodeJobDto from(TranscodeJob job) {
        return new TranscodeJobDto(
            job.getId(),
            job.getVideo().getId(),
            job.getVideo().getTitle().getTitle(),
            job.getVideo().getVideoTitle(),
            job.getStatus(),
            job.getProgressPercent(),
            job.isForce(),
            job.getWorkItem(),
            job.getParentJobId(),
            job.getWorkerName(),
            job.getRequestedAt(),
            job.getStartedAt(),
            job.getFinishedAt(),
            job.getMessage(),
            job.getOutputPath()
        );
    }
}

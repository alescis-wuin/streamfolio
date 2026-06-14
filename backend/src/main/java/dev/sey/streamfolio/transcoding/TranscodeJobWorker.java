package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.nio.file.Path;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TranscodeJobWorker {
    private final TranscodeJobRepository jobs;
    private final MediaAssetRepository assets;
    private final TranscodingService transcodingService;
    private final MediaStorageService mediaStorage;

    public TranscodeJobWorker(TranscodeJobRepository jobs,
                              MediaAssetRepository assets,
                              TranscodingService transcodingService,
                              MediaStorageService mediaStorage) {
        this.jobs = jobs;
        this.assets = assets;
        this.transcodingService = transcodingService;
        this.mediaStorage = mediaStorage;
    }

    @Async
    public void run(Long jobId) {
        markRunning(jobId, "Transcodage demarre.");
        try {
            TranscodeJob job = jobs.findById(jobId).orElseThrow();
            HlsTranscodeResult result = transcodingService.transcodeToHlsAndThumbnails(
                job.getVideo().getId(),
                job.isForce(),
                (progress, message) -> updateProgress(jobId, progress, message)
            );
            markAssetReady(job, result.playlist(), mediaStorage.thumbnailManifest(job.getVideo().getId()));
            markDone(jobId, result.playlist().toString(), result.message());
        } catch (Exception exception) {
            markFailed(jobId, safeMessage(exception));
        }
    }

    private void markRunning(Long jobId, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markRunning(message);
            jobs.save(job);
        });
    }

    private void updateProgress(Long jobId, int progress, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.updateProgress(progress, message);
            jobs.save(job);
        });
    }

    private void markDone(Long jobId, String outputPath, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markDone(outputPath, message);
            jobs.save(job);
        });
    }

    private void markFailed(Long jobId, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markFailed(message);
            jobs.save(job);
            assets.findByVideo(job.getVideo()).ifPresent(asset -> {
                asset.markFailed();
                assets.save(asset);
            });
        });
    }

    private void markAssetReady(TranscodeJob job, Path playlist, Path thumbnailManifest) {
        MediaAsset asset = assets.findByVideo(job.getVideo()).orElseGet(() -> new MediaAsset(job.getVideo()));
        asset.markReady(playlist.toString(), thumbnailManifest.toString());
        assets.save(asset);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}

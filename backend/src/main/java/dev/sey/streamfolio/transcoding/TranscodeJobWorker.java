package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TranscodeJobWorker {
    private final TranscodeJobRepository jobs;
    private final MediaAssetRepository assets;
    private final TranscodingService transcodingService;
    private final MediaStorageService mediaStorage;
    private final Duration initialRetryDelay;
    private final Duration maxRetryDelay;

    public TranscodeJobWorker(TranscodeJobRepository jobs,
                              MediaAssetRepository assets,
                              TranscodingService transcodingService,
                              MediaStorageService mediaStorage,
                              @Value("${streamfolio.transcoding.initial-retry-delay:PT5S}") Duration initialRetryDelay,
                              @Value("${streamfolio.transcoding.max-retry-delay:PT2M}") Duration maxRetryDelay) {
        this.jobs = jobs;
        this.assets = assets;
        this.transcodingService = transcodingService;
        this.mediaStorage = mediaStorage;
        this.initialRetryDelay = initialRetryDelay == null ? Duration.ofSeconds(5) : initialRetryDelay;
        this.maxRetryDelay = maxRetryDelay == null ? Duration.ofMinutes(2) : maxRetryDelay;
    }

    @Async("transcodeTaskExecutor")
    public void run(Long jobId) {
        TranscodeJob job = jobs.findWithVideoById(jobId).orElse(null);
        if (job == null || job.getStatus().isTerminal() || job.isBatch()) {
            return;
        }
        if (job.isCancellationRequested()) {
            markCancelled(jobId, "Job annule avant execution.");
            reconcileParent(job.getParentJobId());
            return;
        }
        markRunning(jobId, "Worker " + job.getWorkItem() + " demarre.");
        try {
            HlsTranscodeResult result = runWorkItem(job);
            markDone(jobId, result.playlist().toString(), result.message());
            reconcileParent(job.getParentJobId());
        } catch (TranscodeCancelledException exception) {
            markCancelled(jobId, safeMessage(exception));
            reconcileParent(job.getParentJobId());
        } catch (Exception exception) {
            markRetryOrFailed(jobId, safeMessage(exception));
            reconcileParent(job.getParentJobId());
        }
    }

    private HlsTranscodeResult runWorkItem(TranscodeJob job) {
        String workItem = job.getWorkItem();
        Long videoId = job.getVideo().getId();
        if (TranscodeJob.WORK_ITEM_THUMBNAILS.equals(workItem)) {
            return transcodingService.generateTimelineThumbnails(videoId, job.isForce(), (progress, message) -> updateProgress(job.getId(), progress, message));
        }
        return transcodingService.transcodeVariant(videoId, workItem, job.isForce(), (progress, message) -> updateProgress(job.getId(), progress, message));
    }

    private void reconcileParent(Long parentJobId) {
        if (parentJobId == null) {
            return;
        }
        List<TranscodeJob> children = jobs.findByParentJobIdOrderByRequestedAtAscIdAsc(parentJobId);
        if (children.isEmpty()) {
            return;
        }
        updateParentProgress(parentJobId, children);
        if (children.stream().anyMatch(child -> child.getStatus() == TranscodeJobStatus.CANCELLED)) {
            markParentCancelled(parentJobId, "Au moins un travail de transcodage a ete annule.");
            return;
        }
        if (children.stream().anyMatch(child -> child.getStatus() == TranscodeJobStatus.FAILED)) {
            markParentFailed(parentJobId, "Au moins une variante de transcodage a echoue.");
            return;
        }
        if (children.stream().allMatch(child -> child.getStatus() == TranscodeJobStatus.DONE)) {
            markParentReady(parentJobId);
        }
    }

    private void markParentReady(Long parentJobId) {
        jobs.findWithVideoById(parentJobId).ifPresent(parent -> {
            try {
                HlsTranscodeResult result = transcodingService.completeHls(parent.getVideo().getId(), parent.isForce());
                markAssetReady(parent, result.playlist(), mediaStorage.thumbnailManifest(parent.getVideo().getId()));
                parent.markDone(result.playlist().toString(), result.message());
                jobs.save(parent);
            } catch (Exception exception) {
                parent.markFailed(safeMessage(exception));
                jobs.save(parent);
                assets.findByVideo(parent.getVideo()).ifPresent(asset -> {
                    asset.markFailed();
                    assets.save(asset);
                });
            }
        });
    }

    private void markParentFailed(Long parentJobId, String message) {
        jobs.findWithVideoById(parentJobId).ifPresent(parent -> {
            parent.markFailed(message);
            jobs.save(parent);
            assets.findByVideo(parent.getVideo()).ifPresent(asset -> {
                asset.markFailed();
                assets.save(asset);
            });
        });
    }

    private void markParentCancelled(Long parentJobId, String message) {
        jobs.findWithVideoById(parentJobId).ifPresent(parent -> {
            parent.markCancelled(message);
            jobs.save(parent);
        });
    }

    private void updateParentProgress(Long parentJobId, List<TranscodeJob> children) {
        int average = (int) Math.round(children.stream().mapToInt(TranscodeJob::getProgressPercent).average().orElse(0));
        jobs.findById(parentJobId).ifPresent(parent -> {
            if (parent.getStatus().isTerminal()) {
                return;
            }
            parent.updateProgress(average, "Progression globale des variantes: " + average + "%. ");
            jobs.save(parent);
        });
    }

    private void markRunning(Long jobId, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markRunning(message, Thread.currentThread().getName());
            jobs.save(job);
        });
    }

    private void updateProgress(Long jobId, int progress, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.updateProgress(progress, message);
            jobs.save(job);
            if (job.getParentJobId() != null) {
                updateParentProgress(job.getParentJobId(), jobs.findByParentJobIdOrderByRequestedAtAscIdAsc(job.getParentJobId()));
            }
        });
    }

    private void markDone(Long jobId, String outputPath, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markDone(outputPath, message);
            jobs.save(job);
        });
    }

    private void markRetryOrFailed(Long jobId, String message) {
        jobs.findWithVideoById(jobId).ifPresent(job -> {
            if (job.canRetry()) {
                Duration delay = retryDelay(job);
                job.markQueuedForRetry(delay, "Nouvel essai dans " + delay.toSeconds() + " s apres echec: " + message);
            } else {
                job.markFailed(message);
            }
            jobs.save(job);
        });
    }

    private void markCancelled(Long jobId, String message) {
        jobs.findById(jobId).ifPresent(job -> {
            job.markCancelled(message);
            jobs.save(job);
        });
    }

    private void markAssetReady(TranscodeJob job, Path playlist, Path thumbnailManifest) {
        MediaAsset asset = assets.findByVideo(job.getVideo()).orElseGet(() -> new MediaAsset(job.getVideo()));
        asset.markReady(playlist.toString(), thumbnailManifest.toString());
        assets.save(asset);
    }

    private Duration retryDelay(TranscodeJob job) {
        long multiplier = 1L << Math.max(0, Math.min(10, job.getAttemptCount() - 1));
        long seconds = Math.min(maxRetryDelay.toSeconds(), Math.max(1, initialRetryDelay.toSeconds()) * multiplier);
        return Duration.ofSeconds(seconds);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}

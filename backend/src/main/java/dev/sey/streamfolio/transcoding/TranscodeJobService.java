package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.MediaAssetStatus;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TranscodeJobService {
    private static final List<TranscodeJobStatus> ACTIVE_BATCH_STATUSES = List.of(
        TranscodeJobStatus.PENDING,
        TranscodeJobStatus.RUNNING,
        TranscodeJobStatus.RETRYING,
        TranscodeJobStatus.CANCELLING
    );

    private final CatalogService catalogService;
    private final TranscodeJobRepository jobs;
    private final MediaAssetRepository assets;
    private final TranscodeJobWorker worker;
    private final TranscodingService transcodingService;
    private final TranscodeCancellationRegistry cancellationRegistry;
    private final int maxAttempts;
    private final int schedulerBatchSize;
    private final Duration restartRetryDelay;

    public TranscodeJobService(CatalogService catalogService,
                               TranscodeJobRepository jobs,
                               MediaAssetRepository assets,
                               TranscodeJobWorker worker,
                               TranscodingService transcodingService,
                               TranscodeCancellationRegistry cancellationRegistry,
                               @Value("${streamfolio.transcoding.max-attempts:3}") int maxAttempts,
                               @Value("${streamfolio.transcoding.scheduler-batch-size:16}") int schedulerBatchSize,
                               @Value("${streamfolio.transcoding.restart-retry-delay:PT10S}") Duration restartRetryDelay) {
        this.catalogService = catalogService;
        this.jobs = jobs;
        this.assets = assets;
        this.worker = worker;
        this.transcodingService = transcodingService;
        this.cancellationRegistry = cancellationRegistry;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.schedulerBatchSize = Math.max(1, schedulerBatchSize);
        this.restartRetryDelay = restartRetryDelay == null ? Duration.ofSeconds(10) : restartRetryDelay;
    }

    @Transactional
    public synchronized TranscodeJobDto submit(Long videoId, boolean force) {
        CatalogVideo video = catalogService.findVideo(videoId);
        List<TranscodeJob> activeBatches = jobs.findActiveByVideoAndWorkItem(
            video,
            TranscodeJob.WORK_ITEM_BATCH,
            ACTIVE_BATCH_STATUSES,
            PageRequest.of(0, 1)
        );
        if (!activeBatches.isEmpty()) {
            return TranscodeJobDto.from(activeBatches.get(0));
        }

        MediaAsset asset = assets.findByVideo(video).orElseGet(() -> assets.save(new MediaAsset(video)));
        if (!force && asset.getStatus() == MediaAssetStatus.READY) {
            TranscodeJob job = jobs.save(newJob(video, false, TranscodeJob.WORK_ITEM_READY, null));
            job.markDone(asset.getHlsMasterPath(), "Asset media deja pret.");
            return TranscodeJobDto.from(jobs.save(job));
        }

        TranscodeJob parent = jobs.saveAndFlush(newJob(video, force, TranscodeJob.WORK_ITEM_BATCH, null));
        parent.markRunning("Planification des jobs par qualite.");
        jobs.save(parent);

        List<TranscodeJob> children = new ArrayList<>();
        for (String variant : transcodingService.variantNames()) {
            children.add(newJob(video, force, variant, parent.getId()));
        }
        children.add(newJob(video, force, TranscodeJob.WORK_ITEM_THUMBNAILS, parent.getId()));
        jobs.saveAll(children);
        jobs.flush();
        runAfterCommit(children.stream().map(TranscodeJob::getId).toList());
        return TranscodeJobDto.from(parent);
    }

    @Transactional(readOnly = true)
    public List<TranscodeJobDto> recentJobs() {
        return jobs.findTop25ByOrderByRequestedAtDesc().stream()
            .map(TranscodeJobDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TranscodeJobDto job(Long jobId) {
        return TranscodeJobDto.from(findJob(jobId));
    }

    @Transactional(readOnly = true)
    public List<MediaAssetDto> assets() {
        return assets.findAll().stream()
            .map(MediaAssetDto::from)
            .toList();
    }

    @Transactional
    public TranscodeJobDto cancel(Long jobId) {
        TranscodeJob job = findJob(jobId);
        job.requestCancellation();
        cancellationRegistry.request(job.getId());
        jobs.save(job);
        if (job.isBatch()) {
            for (TranscodeJob child : jobs.findByParentJobIdOrderByRequestedAtAscIdAsc(job.getId())) {
                child.requestCancellation();
                cancellationRegistry.request(child.getId());
                jobs.save(child);
            }
        }
        return TranscodeJobDto.from(job);
    }

    @Transactional
    public TranscodeJobDto retry(Long jobId) {
        TranscodeJob job = findJob(jobId);
        if (job.isBatch()) {
            return submit(job.getVideo().getId(), true);
        }
        job.resetForManualRetry(maxAttempts);
        cancellationRegistry.clear(job.getId());
        jobs.save(job);
        runAfterCommit(List.of(job.getId()));
        return TranscodeJobDto.from(job);
    }

    @Transactional
    public int requeueInterruptedJobs() {
        List<TranscodeJob> interrupted = jobs.findByStatusIn(List.of(TranscodeJobStatus.RUNNING, TranscodeJobStatus.CANCELLING));
        int count = 0;
        for (TranscodeJob job : interrupted) {
            if (job.isBatch()) {
                continue;
            }
            if (job.getStatus() == TranscodeJobStatus.CANCELLING || job.isCancellationRequested()) {
                job.markCancelled("Job annule pendant un arret backend.");
            } else if (job.canRetry()) {
                job.markQueuedForRetry(restartRetryDelay, "Job interrompu par un arret backend. Relance programmee.");
            } else {
                job.markFailed("Job interrompu par un arret backend sans tentative restante.");
            }
            jobs.save(job);
            count++;
        }
        return count;
    }

    public int dispatchRunnableJobs() {
        List<TranscodeJob> runnable = jobs.findRunnableJobs(
            List.of(TranscodeJobStatus.PENDING, TranscodeJobStatus.RETRYING),
            Instant.now(),
            PageRequest.of(0, schedulerBatchSize)
        );
        runnable.forEach(job -> worker.run(job.getId()));
        return runnable.size();
    }

    private TranscodeJob findJob(Long jobId) {
        return jobs.findWithVideoById(jobId)
            .orElseThrow(() -> new NotFoundException("Job de transcodage introuvable: " + jobId));
    }

    private TranscodeJob newJob(CatalogVideo video, boolean force, String workItem, Long parentJobId) {
        TranscodeJob job = new TranscodeJob(video, force, workItem, parentJobId);
        job.configureRetries(maxAttempts);
        return job;
    }

    private void runAfterCommit(List<Long> jobIds) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            jobIds.forEach(worker::run);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobIds.forEach(worker::run);
            }
        });
    }
}

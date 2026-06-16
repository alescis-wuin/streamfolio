package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.MediaAssetStatus;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TranscodeJobService {
    private final CatalogService catalogService;
    private final TranscodeJobRepository jobs;
    private final MediaAssetRepository assets;
    private final TranscodeJobWorker worker;
    private final TranscodingService transcodingService;

    public TranscodeJobService(CatalogService catalogService,
                               TranscodeJobRepository jobs,
                               MediaAssetRepository assets,
                               TranscodeJobWorker worker,
                               TranscodingService transcodingService) {
        this.catalogService = catalogService;
        this.jobs = jobs;
        this.assets = assets;
        this.worker = worker;
        this.transcodingService = transcodingService;
    }

    @Transactional
    public TranscodeJobDto submit(Long videoId, boolean force) {
        CatalogVideo video = catalogService.findVideo(videoId);
        MediaAsset asset = assets.findByVideo(video).orElseGet(() -> assets.save(new MediaAsset(video)));
        if (!force && asset.getStatus() == MediaAssetStatus.READY) {
            TranscodeJob job = jobs.save(new TranscodeJob(video, false, TranscodeJob.WORK_ITEM_READY, null));
            job.markDone(asset.getHlsMasterPath(), "Asset media deja pret.");
            return TranscodeJobDto.from(jobs.save(job));
        }

        TranscodeJob parent = jobs.saveAndFlush(new TranscodeJob(video, force, TranscodeJob.WORK_ITEM_BATCH, null));
        parent.markRunning("Planification des jobs par qualite.");
        jobs.save(parent);

        List<TranscodeJob> children = new ArrayList<>();
        for (String variant : transcodingService.variantNames()) {
            children.add(new TranscodeJob(video, force, variant, parent.getId()));
        }
        children.add(new TranscodeJob(video, force, TranscodeJob.WORK_ITEM_THUMBNAILS, parent.getId()));
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
        return TranscodeJobDto.from(jobs.findWithVideoById(jobId)
            .orElseThrow(() -> new NotFoundException("Job de transcodage introuvable: " + jobId)));
    }

    @Transactional(readOnly = true)
    public List<MediaAssetDto> assets() {
        return assets.findAll().stream()
            .map(MediaAssetDto::from)
            .toList();
    }

    private void runAfterCommit(List<Long> jobIds) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobIds.forEach(worker::run);
            }
        });
    }
}

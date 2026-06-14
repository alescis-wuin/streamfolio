package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscodeJobService {
    private final CatalogService catalogService;
    private final TranscodeJobRepository jobs;
    private final MediaAssetRepository assets;
    private final TranscodeJobWorker worker;

    public TranscodeJobService(CatalogService catalogService,
                               TranscodeJobRepository jobs,
                               MediaAssetRepository assets,
                               TranscodeJobWorker worker) {
        this.catalogService = catalogService;
        this.jobs = jobs;
        this.assets = assets;
        this.worker = worker;
    }

    @Transactional
    public TranscodeJobDto submit(Long videoId, boolean force) {
        CatalogVideo video = catalogService.findVideo(videoId);
        MediaAsset asset = assets.findByVideo(video).orElseGet(() -> assets.save(new MediaAsset(video)));
        if (!force && asset.getStatus() == dev.sey.streamfolio.domain.MediaAssetStatus.READY) {
            TranscodeJob job = jobs.save(new TranscodeJob(video, false));
            job.markDone(asset.getHlsMasterPath(), "Asset media deja pret.");
            return TranscodeJobDto.from(jobs.save(job));
        }
        TranscodeJob job = jobs.save(new TranscodeJob(video, force));
        worker.run(job.getId());
        return TranscodeJobDto.from(job);
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
}

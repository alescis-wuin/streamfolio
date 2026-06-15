package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.domain.MediaAssetStatus;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TranscodeJobWorkerTest {
    @Mock
    private TranscodeJobRepository jobs;

    @Mock
    private MediaAssetRepository assets;

    @Mock
    private TranscodingService transcodingService;

    @Mock
    private MediaStorageService mediaStorage;

    private TranscodeJobWorker worker;
    private CatalogVideo video;
    private TranscodeJob job;

    @BeforeEach
    void setUp() {
        worker = new TranscodeJobWorker(jobs, assets, transcodingService, mediaStorage);
        CatalogTitle title = new CatalogTitle("worker-demo", "Worker Demo", ContentType.MOVIE, 2026, "TV-PG", 1,
            "Demo", "Demo", "/poster.svg", "/backdrop.svg", 1);
        video = new CatalogVideo(0, 0, "Film", "Worker Video", 12, "worker.mp4", "worker.vtt");
        title.addVideo(video);
        ReflectionTestUtils.setField(video, "id", 7L);
        job = new TranscodeJob(video, true);
        ReflectionTestUtils.setField(job, "id", 11L);
    }

    @Test
    void marksJobDoneAndAssetReadyWhenTranscodingSucceeds() {
        when(jobs.findById(11L)).thenReturn(Optional.of(job));
        when(jobs.findWithVideoById(11L)).thenReturn(Optional.of(job));
        when(mediaStorage.thumbnailManifest(7L)).thenReturn(Path.of("thumbs/manifest.json"));
        when(assets.findByVideo(video)).thenReturn(Optional.empty());
        when(transcodingService.transcodeToHlsAndThumbnails(eq(7L), eq(true), any()))
            .thenAnswer(invocation -> {
                TranscodingService.ProgressReporter reporter = invocation.getArgument(2);
                reporter.report(45, "milieu");
                return new HlsTranscodeResult(7L, true, Path.of("source.mp4"), Path.of("hls/7"), Path.of("hls/7/master.m3u8"), "ok");
            });

        worker.run(11L);

        assertThat(job.getStatus()).isEqualTo(TranscodeJobStatus.DONE);
        assertThat(job.getProgressPercent()).isEqualTo(100);
        verify(assets).save(any(MediaAsset.class));
        verify(jobs).save(job);
    }

    @Test
    void marksJobAndAssetFailedWhenTranscodingFails() {
        MediaAsset asset = new MediaAsset(video);
        asset.markReady("hls/master.m3u8", "thumbs/manifest.json");
        when(jobs.findById(11L)).thenReturn(Optional.of(job));
        when(jobs.findWithVideoById(11L)).thenReturn(Optional.of(job));
        when(assets.findByVideo(video)).thenReturn(Optional.of(asset));
        when(transcodingService.transcodeToHlsAndThumbnails(eq(7L), eq(true), any()))
            .thenThrow(new BadRequestException("ffmpeg failed"));

        worker.run(11L);

        assertThat(job.getStatus()).isEqualTo(TranscodeJobStatus.FAILED);
        assertThat(job.getMessage()).contains("ffmpeg failed");
        assertThat(asset.getStatus()).isEqualTo(MediaAssetStatus.FAILED);
        verify(assets).save(asset);
    }
}

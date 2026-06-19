package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.TranscodeJob;
import dev.sey.streamfolio.domain.TranscodeJobStatus;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TranscodeJobServiceTest {
    @Mock
    private CatalogService catalogService;

    @Mock
    private TranscodeJobRepository jobs;

    @Mock
    private MediaAssetRepository assets;

    @Mock
    private TranscodeJobWorker worker;

    @Mock
    private TranscodingService transcodingService;

    @Mock
    private TranscodeCancellationRegistry cancellationRegistry;

    private TranscodeJobService service;
    private CatalogVideo video;

    @BeforeEach
    void setUp() {
        service = new TranscodeJobService(
            catalogService,
            jobs,
            assets,
            worker,
            transcodingService,
            cancellationRegistry,
            3,
            16,
            Duration.ofSeconds(10)
        );
        CatalogTitle title = new CatalogTitle("dedup-demo", "Dedup Demo", ContentType.MOVIE, 2026, "TV-PG", 1,
            "Demo", "Demo", "/poster.svg", "/backdrop.svg", 1);
        video = new CatalogVideo(0, 0, "Film", "Dedup Video", 12, "dedup.mp4", "dedup.vtt");
        title.addVideo(video);
        ReflectionTestUtils.setField(video, "id", 7L);
    }

    @Test
    void returnsExistingActiveBatchInsteadOfCreatingDuplicateJobs() {
        TranscodeJob activeBatch = new TranscodeJob(video, true, TranscodeJob.WORK_ITEM_BATCH, null);
        ReflectionTestUtils.setField(activeBatch, "id", 33L);
        activeBatch.markRunning("Transcodage deja actif.");

        when(catalogService.findVideo(7L)).thenReturn(video);
        when(jobs.findActiveByVideoAndWorkItem(eq(video), eq(TranscodeJob.WORK_ITEM_BATCH), anyCollection(), any(Pageable.class)))
            .thenReturn(List.of(activeBatch));

        TranscodeJobDto result = service.submit(7L, true);

        assertThat(result.id()).isEqualTo(33L);
        assertThat(result.status()).isEqualTo(TranscodeJobStatus.RUNNING);
        verify(jobs, never()).saveAndFlush(any(TranscodeJob.class));
        verify(worker, never()).run(anyLong());
    }
}

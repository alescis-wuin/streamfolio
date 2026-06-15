package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscodingServiceTest {
    @TempDir
    private Path tempDir;

    @Mock
    private CatalogService catalogService;

    private TranscodingService transcodingService;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(tempDir.resolve("originals"));
        Files.createDirectories(tempDir.resolve("hls"));
        Files.createDirectories(tempDir.resolve("thumbnails"));
        Files.writeString(tempDir.resolve("originals/aurora-drift.mp4"), "demo video");
        transcodingService = new TranscodingService(
            catalogService,
            new MediaStorageService("local", tempDir.toString()),
            new FakeFfmpegService(),
            Duration.ofSeconds(5),
            4
        );
    }

    @Test
    void transcodesLocalOriginalToMultiBitrateHlsPlaylistAndTimelineThumbnails() {
        when(catalogService.findVideo(1L)).thenReturn(video());

        HlsTranscodeResult result = transcodingService.transcodeToHls(1L, true);

        assertThat(result.generated()).isTrue();
        assertThat(result.playlist()).exists();
        assertThat(result.outputDirectory().resolve("360p/segment_000.ts")).exists();
        assertThat(result.outputDirectory().resolve("720p/segment_000.ts")).exists();
        assertThat(result.outputDirectory().resolve("1080p/segment_000.ts")).exists();
        assertThat(read(result.playlist()))
            .contains("EXTM3U", "EXT-X-STREAM-INF", "360p/playlist.m3u8", "720p/playlist.m3u8", "1080p/playlist.m3u8");
        assertThat(tempDir.resolve("thumbnails/1/manifest.json")).exists();
        assertThat(tempDir.resolve("thumbnails/1/thumb_000.jpg")).exists();
        assertThat(read(tempDir.resolve("thumbnails/1/manifest.json"))).contains("timeSeconds", "/api/videos/1/thumbnails/thumb_000.jpg");
    }

    @Test
    void skipsExistingHlsAndThumbnailsWhenForceIsFalse() {
        when(catalogService.findVideo(1L)).thenReturn(video());

        HlsTranscodeResult first = transcodingService.transcodeToHls(1L, true);
        HlsTranscodeResult second = transcodingService.transcodeToHls(1L, false);

        assertThat(first.generated()).isTrue();
        assertThat(second.generated()).isFalse();
        assertThat(second.message()).contains("presentes");
    }

    private CatalogVideo video() {
        return new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static class FakeFfmpegService extends FfmpegService {
        FakeFfmpegService() {
            super("fake-binary", Duration.ofSeconds(1));
        }

        @Override
        public CommandResult runOrThrow(List<String> command, Duration timeout) {
            Path output = Path.of(command.get(command.size() - 1));
            Path outputDirectory = output.getParent();
            try {
                Files.createDirectories(outputDirectory);
                if (output.toString().endsWith(".jpg")) {
                    Files.writeString(output, "fake jpg");
                    return new CommandResult(0, "ok", false);
                }
                String mark = Character.toString((char) 35);
                String content = mark + "EXTM3U\n"
                    + mark + "EXT-X-VERSION:3\n"
                    + mark + "EXT-X-PLAYLIST-TYPE:VOD\n"
                    + mark + "EXTINF:1.0,\n"
                    + "segment_000.ts\n"
                    + mark + "EXT-X-ENDLIST\n";
                Files.writeString(output, content);
                Files.writeString(outputDirectory.resolve("segment_000.ts"), "fake segment");
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            return new CommandResult(0, "ok", false);
        }
    }
}

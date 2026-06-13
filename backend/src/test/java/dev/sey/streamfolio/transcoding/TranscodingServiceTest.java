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
    void transcodesLocalOriginalToHlsPlaylist() {
        when(catalogService.findVideo(1L)).thenReturn(video());

        HlsTranscodeResult result = transcodingService.transcodeToHls(1L, true);

        assertThat(result.generated()).isTrue();
        assertThat(result.playlist()).exists();
        assertThat(result.outputDirectory().resolve("segment_000.ts")).exists();
        assertThat(read(result.playlist())).contains("EXTM3U", "PLAYLIST-TYPE:VOD");
    }

    @Test
    void skipsExistingHlsPlaylistWhenForceIsFalse() {
        when(catalogService.findVideo(1L)).thenReturn(video());

        HlsTranscodeResult first = transcodingService.transcodeToHls(1L, true);
        HlsTranscodeResult second = transcodingService.transcodeToHls(1L, false);

        assertThat(first.generated()).isTrue();
        assertThat(second.generated()).isFalse();
        assertThat(second.message()).contains("déjà présente");
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
            Path playlist = Path.of(command.get(command.size() - 1));
            Path outputDirectory = playlist.getParent();
            try {
                Files.createDirectories(outputDirectory);
                String mark = Character.toString((char) 35);
                String content = mark + "EXTM3U\n"
                    + mark + "EXT-X-VERSION:3\n"
                    + mark + "EXT-X-PLAYLIST-TYPE:VOD\n"
                    + mark + "EXTINF:1.0,\n"
                    + "segment_000.ts\n"
                    + mark + "EXT-X-ENDLIST\n";
                Files.writeString(playlist, content);
                Files.writeString(outputDirectory.resolve("segment_000.ts"), "fake segment");
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            return new CommandResult(0, "ok", false);
        }
    }
}

package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class TranscodingServiceIntegrationTest {
    private static final Path MEDIA_ROOT = createTempMediaRoot();
    private static final Path FFMPEG = createFakeFfmpegBinary();

    @Autowired
    private TranscodingService transcodingService;

    @DynamicPropertySource
    static void transcodingProperties(DynamicPropertyRegistry registry) {
        registry.add("streamfolio.media.storage", () -> "local");
        registry.add("streamfolio.media.root", MEDIA_ROOT::toString);
        registry.add("streamfolio.ffmpeg.binary", FFMPEG::toString);
        registry.add("streamfolio.ffmpeg.transcode-timeout", () -> "PT5S");
    }

    @BeforeEach
    void setUp() throws IOException {
        prepareLocalMedia();
    }

    @Test
    void transcodesLocalOriginalToHlsPlaylist() {
        HlsTranscodeResult result = transcodingService.transcodeToHls(1L, true);

        assertThat(result.generated()).isTrue();
        assertThat(result.playlist()).exists();
        assertThat(result.outputDirectory().resolve("segment_000.ts")).exists();
        assertThat(read(result.playlist())).contains("#EXTM3U", "#EXT-X-PLAYLIST-TYPE:VOD");
    }

    @Test
    void skipsExistingHlsPlaylistWhenForceIsFalse() {
        HlsTranscodeResult first = transcodingService.transcodeToHls(1L, true);
        HlsTranscodeResult second = transcodingService.transcodeToHls(1L, false);

        assertThat(first.generated()).isTrue();
        assertThat(second.generated()).isFalse();
        assertThat(second.message()).contains("déjà présente");
    }

    private static Path createTempMediaRoot() {
        try {
            return Files.createTempDirectory("streamfolio-hls-test");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create media root", exception);
        }
    }

    private static Path createFakeFfmpegBinary() {
        try {
            Path binary = Files.createTempFile("streamfolio-fake-ffmpeg", "");
            Files.writeString(binary, """
#!/usr/bin/env bash
out="${@: -1}"
dir="$(dirname "$out")"
mkdir -p "$dir"
cat > "$out" <<'EOF'
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-PLAYLIST-TYPE:VOD
#EXTINF:1.000000,
segment_000.ts
#EXT-X-ENDLIST
EOF
printf 'fake segment' > "$dir/segment_000.ts"
""");
            if (!binary.toFile().setExecutable(true)) {
                throw new IOException("Unable to mark fake ffmpeg as executable");
            }
            return binary;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create fake ffmpeg", exception);
        }
    }

    private static void prepareLocalMedia() throws IOException {
        Files.createDirectories(MEDIA_ROOT.resolve("originals"));
        Files.createDirectories(MEDIA_ROOT.resolve("subtitles"));
        Files.createDirectories(MEDIA_ROOT.resolve("hls"));
        copyClasspathMedia("media/aurora-drift.mp4", MEDIA_ROOT.resolve("originals/aurora-drift.mp4"));
    }

    private static void copyClasspathMedia(String source, Path target) throws IOException {
        try (InputStream input = new ClassPathResource(source).getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

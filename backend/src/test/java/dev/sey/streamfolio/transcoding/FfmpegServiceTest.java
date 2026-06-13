package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FfmpegServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void reportsUnavailableWhenBinaryDoesNotExist() {
        FfmpegService ffmpeg = new FfmpegService("missing-ffmpeg-for-streamfolio-test", Duration.ofMillis(250));

        FfmpegStatus status = ffmpeg.status();

        assertThat(status.available()).isFalse();
        assertThat(status.binary()).isEqualTo("missing-ffmpeg-for-streamfolio-test");
        assertThat(status.error()).isNotBlank();
    }

    @Test
    void detectsFakeFfmpegBinary() throws IOException {
        Path binary = tempDir.resolve("fake-ffmpeg");
        Files.writeString(binary, "#!/usr/bin/env bash\necho 'ffmpeg version fake-1.0'\n");
        assertThat(binary.toFile().setExecutable(true)).isTrue();

        FfmpegService ffmpeg = new FfmpegService(binary.toString(), Duration.ofSeconds(2));

        FfmpegStatus status = ffmpeg.status();

        assertThat(status.available()).isTrue();
        assertThat(status.version()).contains("ffmpeg version fake-1.0");
    }
}

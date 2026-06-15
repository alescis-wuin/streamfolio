package dev.sey.streamfolio.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
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

    @Test
    void runOrThrowReportsNonZeroExitCode() throws IOException {
        Path binary = tempDir.resolve("failing-ffmpeg");
        Files.writeString(binary, "#!/usr/bin/env bash\necho 'boom'\nexit 42\n");
        assertThat(binary.toFile().setExecutable(true)).isTrue();
        FfmpegService ffmpeg = new FfmpegService(binary.toString(), Duration.ofSeconds(2));

        assertThatThrownBy(() -> ffmpeg.runOrThrow(List.of(binary.toString()), Duration.ofSeconds(2)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("FFmpeg a")
            .hasMessageContaining("boom");
    }

    @Test
    void runReturnsTimeoutResultWhenProcessExceedsDeadline() throws IOException {
        Path binary = tempDir.resolve("slow-ffmpeg");
        Files.writeString(binary, "#!/usr/bin/env bash\nsleep 2\n");
        assertThat(binary.toFile().setExecutable(true)).isTrue();
        FfmpegService ffmpeg = new FfmpegService(binary.toString(), Duration.ofMillis(100));

        FfmpegService.CommandResult result = ffmpeg.run(List.of(binary.toString()), Duration.ofMillis(100));

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(-1);
    }
}

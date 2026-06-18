package dev.sey.streamfolio.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.common.BadRequestException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaSourceValidationServiceTest {
    @TempDir
    private Path tempDir;

    @Mock
    private MediaDurationService probes;

    @Test
    void rejectsInvalidSignatureInStrictMode() throws Exception {
        Path file = tempDir.resolve("invalid.mp4");
        Files.writeString(file, "not an mp4 file");
        StoredMediaFile stored = stored(file, "invalid.mp4");
        MediaSourceValidationService validation = strictValidation();

        when(probes.probeMetadata(file)).thenReturn(metadata("h264", true, true, 10));

        assertThatThrownBy(() -> validation.validateVideo(stored))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Signature");
    }

    @Test
    void acceptsValidSignatureAndCodecInStrictMode() throws Exception {
        Path file = tempDir.resolve("valid.avi");
        Files.write(file, new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'A', 'V', 'I', ' ', 0, 0, 0, 0});
        StoredMediaFile stored = stored(file, "valid.avi");
        MediaSourceValidationService validation = strictValidation();

        when(probes.probeMetadata(file)).thenReturn(metadata("h264", true, true, 10));

        assertThatCode(() -> validation.validateVideo(stored)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedVideoCodec() throws Exception {
        Path file = tempDir.resolve("valid.avi");
        Files.write(file, new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'A', 'V', 'I', ' ', 0, 0, 0, 0});
        StoredMediaFile stored = stored(file, "valid.avi");
        MediaSourceValidationService validation = strictValidation();

        when(probes.probeMetadata(file)).thenReturn(metadata("unsupported", true, true, 10));

        assertThatThrownBy(() -> validation.validateVideo(stored))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Codec");
    }

    private MediaSourceValidationService strictValidation() {
        return new MediaSourceValidationService(probes, true, true, true, true, "h264,hevc");
    }

    private StoredMediaFile stored(Path file, String filename) throws Exception {
        return new StoredMediaFile(filename, filename, "sha", "video/test", Files.size(file), true, null, file);
    }

    private MediaProbeMetadata metadata(String codec, boolean video, boolean audio, Integer duration) {
        return new MediaProbeMetadata(duration, null, null, List.of(), null, "avi", "AVI", null, codec, video, audio, Map.of());
    }
}

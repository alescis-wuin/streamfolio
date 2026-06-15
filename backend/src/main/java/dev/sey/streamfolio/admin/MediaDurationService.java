package dev.sey.streamfolio.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaDurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDurationService.class);

    private final String probeBinary;
    private final Duration timeout;

    public MediaDurationService(@Value("${streamfolio.ffmpeg.probe-binary:ffprobe}") String probeBinary,
                                @Value("${streamfolio.ffmpeg.probe-timeout:PT5S}") Duration timeout) {
        this.probeBinary = probeBinary == null || probeBinary.isBlank() ? "ffprobe" : probeBinary.trim();
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
    }

    public Optional<Integer> detectSeconds(Path mediaPath) {
        if (mediaPath == null || !Files.isRegularFile(mediaPath)) {
            return Optional.empty();
        }
        List<String> command = List.of(
            probeBinary,
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            mediaPath.toString()
        );
        try {
            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process.getInputStream()));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warn("ffprobe timeout while reading duration for {}", mediaPath.getFileName());
                return Optional.empty();
            }
            String raw = outputNow(output).trim();
            if (process.exitValue() != 0) {
                LOGGER.debug("ffprobe failed for {}: {}", mediaPath.getFileName(), raw);
                return Optional.empty();
            }
            return parseDurationSeconds(raw);
        } catch (IOException exception) {
            LOGGER.debug("ffprobe unavailable for duration detection: {}", exception.getMessage());
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<Integer> parseDurationSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            double value = Double.parseDouble(raw.trim().replace(',', '.'));
            if (!Double.isFinite(value) || value <= 0) {
                return Optional.empty();
            }
            return Optional.of(Math.max(1, (int) Math.ceil(value)));
        } catch (NumberFormatException exception) {
            LOGGER.debug("Invalid ffprobe duration output: {}", raw);
            return Optional.empty();
        }
    }

    private static String readOutput(InputStream input) {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return exception.getMessage();
        }
    }

    private static String outputNow(CompletableFuture<String> output) {
        try {
            return output.get(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return "";
        }
    }
}

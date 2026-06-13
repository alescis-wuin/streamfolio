package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FfmpegService {
    private final String binary;
    private final Duration probeTimeout;

    public FfmpegService(@Value("${streamfolio.ffmpeg.binary:ffmpeg}") String binary,
                         @Value("${streamfolio.ffmpeg.probe-timeout:PT5S}") Duration probeTimeout) {
        this.binary = binary == null || binary.isBlank() ? "ffmpeg" : binary.trim();
        this.probeTimeout = probeTimeout == null ? Duration.ofSeconds(5) : probeTimeout;
    }

    public FfmpegStatus status() {
        try {
            CommandResult result = run(List.of(binary, "-version"), probeTimeout);
            if (result.exitCode() == 0) {
                return FfmpegStatus.available(binary, firstLine(result.output()));
            }
            return FfmpegStatus.unavailable(binary, trimOutput(result.output()));
        } catch (IOException exception) {
            return FfmpegStatus.unavailable(binary, exception.getMessage());
        }
    }

    public CommandResult runOrThrow(List<String> command, Duration timeout) {
        try {
            CommandResult result = run(command, timeout);
            if (result.exitCode() != 0) {
                throw new BadRequestException("FFmpeg a échoué: " + trimOutput(result.output()));
            }
            return result;
        } catch (IOException exception) {
            throw new BadRequestException("FFmpeg indisponible: " + exception.getMessage());
        }
    }

    public String binary() {
        return binary;
    }

    CommandResult run(List<String> command, Duration timeout) throws IOException {
        if (command == null || command.isEmpty()) {
            throw new BadRequestException("Commande FFmpeg vide.");
        }
        Duration effectiveTimeout = timeout == null ? Duration.ofMinutes(5) : timeout;
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process.getInputStream()));
        try {
            boolean finished = process.waitFor(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, outputNow(output), true);
            }
            return new CommandResult(process.exitValue(), outputNow(output), false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new CommandResult(-1, outputNow(output), true);
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

    private static String firstLine(String output) {
        String trimmed = trimOutput(output);
        int newline = trimmed.indexOf('\n');
        return newline >= 0 ? trimmed.substring(0, newline).trim() : trimmed;
    }

    private static String trimOutput(String output) {
        if (output == null) {
            return "";
        }
        String trimmed = output.trim();
        return trimmed.length() <= 2000 ? trimmed : trimmed.substring(0, 2000);
    }

    public record CommandResult(int exitCode, String output, boolean timedOut) {
    }
}

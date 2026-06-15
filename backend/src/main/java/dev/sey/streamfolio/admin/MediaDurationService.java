package dev.sey.streamfolio.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.transcoding.FfmpegService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaDurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDurationService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String probeBinary;
    private final Duration timeout;
    private final FfmpegService ffmpeg;

    public MediaDurationService(@Value("${streamfolio.ffmpeg.probe-binary:ffprobe}") String probeBinary,
                                @Value("${streamfolio.ffmpeg.probe-timeout:PT5S}") Duration timeout,
                                FfmpegService ffmpeg) {
        this.probeBinary = probeBinary == null || probeBinary.isBlank() ? "ffprobe" : probeBinary.trim();
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
        this.ffmpeg = ffmpeg;
    }

    public Optional<Integer> detectSeconds(Path mediaPath) {
        return Optional.ofNullable(probeMetadata(mediaPath).durationSeconds());
    }

    public MediaProbeMetadata probeMetadata(Path mediaPath) {
        if (mediaPath == null || !Files.isRegularFile(mediaPath)) {
            return MediaProbeMetadata.empty();
        }
        List<String> command = List.of(
            probeBinary,
            "-v", "error",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            mediaPath.toString()
        );
        try {
            ProcessOutput output = run(command);
            if (output.timedOut()) {
                LOGGER.warn("ffprobe timeout while reading metadata for {}", mediaPath.getFileName());
                return MediaProbeMetadata.empty();
            }
            if (output.exitCode() != 0) {
                LOGGER.debug("ffprobe failed for {}: {}", mediaPath.getFileName(), output.output());
                return MediaProbeMetadata.empty();
            }
            return parseMetadata(output.output());
        } catch (IOException exception) {
            LOGGER.debug("ffprobe unavailable for metadata detection: {}", exception.getMessage());
            return MediaProbeMetadata.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return MediaProbeMetadata.empty();
        }
    }

    public byte[] extractThumbnail(Path mediaPath, double timestampSeconds) {
        if (mediaPath == null || !Files.isRegularFile(mediaPath)) {
            throw new BadRequestException("Fichier video introuvable.");
        }
        Path output = null;
        try {
            output = Files.createTempFile("streamfolio-thumbnail-", ".jpg");
            String timestamp = String.format(Locale.ROOT, "%.3f", Math.max(0, timestampSeconds));
            ffmpeg.runOrThrow(List.of(
                ffmpeg.binary(),
                "-y",
                "-hide_banner",
                "-loglevel", "error",
                "-ss", timestamp,
                "-i", mediaPath.toString(),
                "-frames:v", "1",
                "-q:v", "2",
                output.toString()
            ), timeout);
            if (!Files.isRegularFile(output) || Files.size(output) <= 0) {
                throw new BadRequestException("Impossible d'extraire une miniature depuis cette video.");
            }
            return Files.readAllBytes(output);
        } catch (IOException exception) {
            LOGGER.debug("ffmpeg thumbnail extraction unavailable: {}", exception.getMessage());
            throw new BadRequestException("Impossible d'extraire une miniature depuis cette video.");
        } finally {
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException ignored) {
                    // Best-effort cleanup.
                }
            }
        }
    }

    private MediaProbeMetadata parseMetadata(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return MediaProbeMetadata.empty();
        }
        try {
            JsonNode root = JSON.readTree(rawJson);
            JsonNode format = root.path("format");
            Map<String, String> tags = new LinkedHashMap<>();
            collectTags(format.path("tags"), tags);
            root.path("streams").forEach(stream -> collectTags(stream.path("tags"), tags));

            Integer duration = durationFrom(format, root.path("streams")).orElse(null);
            String title = firstTag(tags, "title");
            String description = firstTag(tags, "description", "synopsis", "summary", "comment");
            List<String> authors = authorsFrom(tags);
            String releaseDate = firstTag(tags, "date", "creation_time", "year", "releasedate", "release_date");
            String encoder = firstTag(tags, "encoder", "encoded_by");
            String formatName = textOrNull(format.path("format_name").asText(null));
            String formatLongName = textOrNull(format.path("format_long_name").asText(null));

            return new MediaProbeMetadata(
                duration,
                title,
                description,
                authors,
                releaseDate,
                formatName,
                formatLongName,
                encoder,
                Map.copyOf(tags)
            );
        } catch (IOException exception) {
            LOGGER.debug("Invalid ffprobe JSON metadata: {}", exception.getMessage());
            return MediaProbeMetadata.empty();
        }
    }

    private Optional<Integer> durationFrom(JsonNode format, JsonNode streams) {
        Optional<Integer> formatDuration = parseDurationSeconds(format.path("duration").asText(""));
        if (formatDuration.isPresent()) {
            return formatDuration;
        }
        int best = 0;
        for (JsonNode stream : streams) {
            Optional<Integer> streamDuration = parseDurationSeconds(stream.path("duration").asText(""));
            if (streamDuration.isPresent()) {
                best = Math.max(best, streamDuration.get());
            }
        }
        return best > 0 ? Optional.of(best) : Optional.empty();
    }

    private Optional<Integer> parseDurationSeconds(String raw) {
        if (raw == null || raw.isBlank() || "N/A".equalsIgnoreCase(raw.trim())) {
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

    private void collectTags(JsonNode tags, Map<String, String> result) {
        if (tags == null || !tags.isObject()) {
            return;
        }
        tags.fields().forEachRemaining(entry -> {
            String key = normalizeTagKey(entry.getKey());
            String value = textOrNull(entry.getValue().asText(null));
            if (key != null && value != null) {
                result.putIfAbsent(key, value);
            }
        });
    }

    private List<String> authorsFrom(Map<String, String> tags) {
        Set<String> result = new LinkedHashSet<>();
        for (String key : List.of("artist", "author", "composer", "director", "producer", "performer", "creator")) {
            String value = firstTag(tags, key);
            if (value != null) {
                for (String part : value.split("[,;]")) {
                    String clean = textOrNull(part);
                    if (clean != null) {
                        result.add(clean);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private String firstTag(Map<String, String> tags, String... keys) {
        for (String key : keys) {
            String value = tags.get(normalizeTagKey(key));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeTagKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ProcessOutput run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process.getInputStream()));
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new ProcessOutput(-1, outputNow(output), true);
        }
        return new ProcessOutput(process.exitValue(), outputNow(output), false);
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

    private record ProcessOutput(int exitCode, String output, boolean timedOut) {
    }
}

record MediaProbeMetadata(Integer durationSeconds,
                          String title,
                          String description,
                          List<String> authors,
                          String releaseDate,
                          String formatName,
                          String formatLongName,
                          String encoder,
                          Map<String, String> tags) {
    static MediaProbeMetadata empty() {
        return new MediaProbeMetadata(null, null, null, List.of(), null, null, null, null, Map.of());
    }
}

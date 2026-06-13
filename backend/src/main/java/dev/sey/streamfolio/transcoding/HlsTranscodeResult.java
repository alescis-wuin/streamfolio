package dev.sey.streamfolio.transcoding;

import java.nio.file.Path;

public record HlsTranscodeResult(
    Long videoId,
    boolean generated,
    Path source,
    Path outputDirectory,
    Path playlist,
    String message
) {
}

package dev.sey.streamfolio.admin;

import java.nio.file.Path;

public record StoredMediaFile(
    String storedFilename,
    String originalFilename,
    String contentSha256,
    String contentType,
    long sizeBytes,
    boolean created,
    String publicPath,
    Path storedPath
) {
}

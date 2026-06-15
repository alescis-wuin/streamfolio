package dev.sey.streamfolio.admin;

public record StoredMediaFile(
    String storedFilename,
    String originalFilename,
    String contentSha256,
    String contentType,
    long sizeBytes,
    boolean created,
    String publicPath
) {
}

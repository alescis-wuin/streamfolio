package dev.sey.streamfolio.admin;

public record AdminVideoLinkRequest(
    Long targetTitleId,
    Integer seasonNumber,
    Integer episodeNumber,
    String label,
    String videoTitle
) {
}

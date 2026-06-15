package dev.sey.streamfolio.admin;

public record AdminVideoOrderRequest(
    Integer seasonNumber,
    Integer episodeNumber,
    String label,
    String videoTitle
) {
}

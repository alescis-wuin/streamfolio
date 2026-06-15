package dev.sey.streamfolio.admin;

import java.util.List;
import java.util.Map;

public record AdminVideoProbeResponse(Integer durationSeconds,
                                      String formattedDuration,
                                      String title,
                                      String description,
                                      List<String> authors,
                                      String releaseDate,
                                      String formatName,
                                      String formatLongName,
                                      String encoder,
                                      Map<String, String> tags) {
    public static AdminVideoProbeResponse from(MediaProbeMetadata metadata) {
        return new AdminVideoProbeResponse(
            metadata.durationSeconds(),
            formatDuration(metadata.durationSeconds()),
            metadata.title(),
            metadata.description(),
            metadata.authors(),
            metadata.releaseDate(),
            metadata.formatName(),
            metadata.formatLongName(),
            metadata.encoder(),
            metadata.tags()
        );
    }

    private static String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00";
        }
        int total = Math.max(0, seconds);
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int remainingSeconds = total % 60;
        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, remainingSeconds);
        }
        return "%02d:%02d".formatted(minutes, remainingSeconds);
    }
}

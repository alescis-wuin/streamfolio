package dev.sey.streamfolio.transcoding;

public record FfmpegStatus(
    boolean available,
    String binary,
    String version,
    String error
) {
    public static FfmpegStatus available(String binary, String version) {
        return new FfmpegStatus(true, binary, version, null);
    }

    public static FfmpegStatus unavailable(String binary, String error) {
        return new FfmpegStatus(false, binary, null, error);
    }
}

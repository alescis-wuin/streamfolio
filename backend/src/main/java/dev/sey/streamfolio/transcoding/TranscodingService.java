package dev.sey.streamfolio.transcoding;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.streaming.MediaStorageMode;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranscodingService {
    private static final List<Variant> VARIANTS = List.of(
        new Variant("360p", 640, 360, "800k", 1_000_000),
        new Variant("720p", 1280, 720, "2800k", 3_200_000),
        new Variant("1080p", 1920, 1080, "5000k", 5_600_000)
    );
    private static final int THUMBNAIL_COUNT = 6;

    private final CatalogService catalogService;
    private final MediaStorageService mediaStorage;
    private final FfmpegService ffmpeg;
    private final Duration transcodeTimeout;
    private final int hlsSegmentTime;

    public TranscodingService(CatalogService catalogService,
                              MediaStorageService mediaStorage,
                              FfmpegService ffmpeg,
                              @Value("${streamfolio.ffmpeg.transcode-timeout:PT5M}") Duration transcodeTimeout,
                              @Value("${streamfolio.ffmpeg.hls-segment-time:4}") int hlsSegmentTime) {
        this.catalogService = catalogService;
        this.mediaStorage = mediaStorage;
        this.ffmpeg = ffmpeg;
        this.transcodeTimeout = transcodeTimeout == null ? Duration.ofMinutes(5) : transcodeTimeout;
        this.hlsSegmentTime = Math.max(1, hlsSegmentTime);
    }

    public HlsTranscodeResult transcodeToHls(Long videoId) {
        return transcodeToHls(videoId, false);
    }

    public HlsTranscodeResult transcodeToHls(Long videoId, boolean force) {
        return transcodeToHlsAndThumbnails(videoId, force, ProgressReporter.noop());
    }

    public HlsTranscodeResult transcodeToHlsAndThumbnails(Long videoId, boolean force, ProgressReporter progress) {
        if (mediaStorage.mode() != MediaStorageMode.LOCAL) {
            throw new BadRequestException("Transcodage HLS disponible uniquement avec le stockage media local.");
        }

        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = mediaStorage.localOriginalPath(video.getAssetFilename());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new NotFoundException("Fichier source introuvable pour transcodage: " + video.getAssetFilename());
        }

        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        Path playlist = mediaStorage.hlsMasterPlaylist(videoId);
        Path thumbnailManifest = mediaStorage.thumbnailManifest(videoId);
        if (Files.isRegularFile(playlist) && Files.isRegularFile(thumbnailManifest) && !force) {
            return new HlsTranscodeResult(videoId, false, source, outputDirectory, playlist, "Sorties HLS et thumbnails deja presentes.");
        }

        progress.report(10, "Preparation des dossiers media.");
        prepareOutputDirectory(outputDirectory);
        for (int index = 0; index < VARIANTS.size(); index++) {
            Variant variant = VARIANTS.get(index);
            Path variantPlaylist = outputDirectory.resolve(variant.name()).resolve("playlist.m3u8");
            prepareVariantDirectory(variantPlaylist.getParent());
            progress.report(20 + index * 15, "Generation HLS " + variant.name() + ".");
            ffmpeg.runOrThrow(command(source, variant, variantPlaylist), transcodeTimeout);
            validatePlaylist(variantPlaylist, false);
        }
        writeMasterPlaylist(playlist);
        validatePlaylist(playlist, true);
        progress.report(72, "Generation des thumbnails timeline.");
        generateTimelineThumbnails(videoId, video, source);
        progress.report(95, "Validation des sorties media.");
        return new HlsTranscodeResult(videoId, true, source, outputDirectory, playlist, "HLS multi-bitrate et thumbnails generes.");
    }

    private void prepareOutputDirectory(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
            cleanDirectory(outputDirectory);
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de preparer le dossier HLS: " + exception.getMessage());
        }
    }

    private void prepareVariantDirectory(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de preparer la variante HLS: " + exception.getMessage());
        }
    }

    private List<String> command(Path source, Variant variant, Path playlist) {
        Path outputDirectory = playlist.getParent();
        return List.of(
            ffmpeg.binary(), "-y", "-i", source.toString(),
            "-map", "0:v:0", "-map", "0:a:0?",
            "-vf", scaleFilter(variant),
            "-c:v", "libx264", "-preset", "veryfast", "-b:v", variant.videoBitrate(),
            "-pix_fmt", "yuv420p", "-flags", "+cgop", "-g", String.valueOf(hlsSegmentTime * 24), "-sc_threshold", "0",
            "-c:a", "aac", "-b:a", "128k", "-ac", "2",
            "-f", "hls", "-hls_time", String.valueOf(hlsSegmentTime), "-hls_list_size", "0", "-hls_playlist_type", "vod",
            "-hls_segment_filename", outputDirectory.resolve("segment_%03d.ts").toString(),
            playlist.toString()
        );
    }

    private List<String> thumbnailCommand(Path source, int second, Path output) {
        return List.of(
            ffmpeg.binary(), "-y", "-ss", String.valueOf(second), "-i", source.toString(),
            "-frames:v", "1", "-q:v", "3", "-vf", "scale=320:-1",
            output.toString()
        );
    }

    private String scaleFilter(Variant variant) {
        return "scale=w=" + variant.width() + ":h=" + variant.height()
            + ":force_original_aspect_ratio=decrease,pad=w=" + variant.width()
            + ":h=" + variant.height() + ":x=(ow-iw)/2:y=(oh-ih)/2:color=black";
    }

    private void generateTimelineThumbnails(Long videoId, CatalogVideo video, Path source) {
        Path directory = mediaStorage.thumbnailDirectory(videoId);
        Path manifest = mediaStorage.thumbnailManifest(videoId);
        try {
            Files.createDirectories(directory);
            cleanDirectory(directory);
            Files.createDirectories(directory);
            List<ThumbnailEntry> entries = new ArrayList<>();
            int duration = Math.max(1, video.getDurationSeconds());
            int step = Math.max(1, duration / THUMBNAIL_COUNT);
            for (int index = 0; index < THUMBNAIL_COUNT; index++) {
                int second = Math.min(Math.max(0, duration - 1), index * step);
                String filename = "thumb_" + String.format("%03d", index) + ".jpg";
                Path output = directory.resolve(filename);
                ffmpeg.runOrThrow(thumbnailCommand(source, second, output), transcodeTimeout);
                entries.add(new ThumbnailEntry(second, "/api/videos/" + videoId + "/thumbnails/" + filename));
            }
            Files.writeString(manifest, manifestJson(videoId, entries));
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de generer les thumbnails: " + exception.getMessage());
        }
    }

    private String manifestJson(Long videoId, List<ThumbnailEntry> entries) {
        StringBuilder builder = new StringBuilder("{\n  \"videoId\": ").append(videoId).append(",\n  \"items\": [\n");
        for (int index = 0; index < entries.size(); index++) {
            ThumbnailEntry entry = entries.get(index);
            builder.append("    { \"timeSeconds\": ").append(entry.timeSeconds())
                .append(", \"url\": \"").append(entry.url()).append("\" }");
            if (index < entries.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        return builder.append("  ]\n}\n").toString();
    }

    private void writeMasterPlaylist(Path playlist) {
        StringBuilder content = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
        for (Variant variant : VARIANTS) {
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                .append(variant.bandwidth())
                .append(",RESOLUTION=")
                .append(variant.width())
                .append('x')
                .append(variant.height())
                .append(",NAME=\"")
                .append(variant.name())
                .append("\"\n")
                .append(variant.name())
                .append("/playlist.m3u8\n");
        }
        try {
            Files.writeString(playlist, content.toString());
        } catch (IOException exception) {
            throw new BadRequestException("Impossible d'ecrire la playlist HLS master: " + exception.getMessage());
        }
    }

    private void validatePlaylist(Path playlist, boolean master) {
        try {
            if (!Files.isRegularFile(playlist) || !Files.isReadable(playlist)) {
                throw new BadRequestException("Playlist HLS non generee.");
            }
            String content = Files.readString(playlist);
            if (!content.contains("#EXTM3U") || (master && !content.contains("#EXT-X-STREAM-INF"))) {
                throw new BadRequestException("Playlist HLS invalide.");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de lire la playlist HLS: " + exception.getMessage());
        }
    }

    private void cleanDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(directory)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public interface ProgressReporter {
        void report(int progressPercent, String message);

        static ProgressReporter noop() {
            return (progressPercent, message) -> { };
        }
    }

    private record Variant(String name, int width, int height, String videoBitrate, int bandwidth) {
    }

    private record ThumbnailEntry(int timeSeconds, String url) {
    }
}

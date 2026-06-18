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
import java.util.Locale;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranscodingService {
    private static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final String DEFAULT_VARIANTS = "360p:640:360:800k:1000000,720p:1280:720:2800k:3200000,1080p:1920:1080:5000k:5600000";

    private final CatalogService catalogService;
    private final MediaStorageService mediaStorage;
    private final FfmpegService ffmpeg;
    private final Duration transcodeTimeout;
    private final int hlsSegmentTime;
    private final String hlsFlags;
    private final String hlsPlaylistType;
    private final int thumbnailCount;
    private final String hardwareAcceleration;
    private final String nvidiaEncoder;
    private final String nvidiaPreset;
    private final String cpuPreset;
    private final List<Variant> variants;

    TranscodingService(CatalogService catalogService,
                       MediaStorageService mediaStorage,
                       FfmpegService ffmpeg,
                       Duration transcodeTimeout,
                       int hlsSegmentTime,
                       String hardwareAcceleration,
                       String nvidiaEncoder) {
        this(catalogService, mediaStorage, ffmpeg, transcodeTimeout, hlsSegmentTime,
            "independent_segments+temp_file", "vod", 6, hardwareAcceleration, nvidiaEncoder,
            "fast", "veryfast", DEFAULT_VARIANTS);
    }

    @Autowired
    public TranscodingService(CatalogService catalogService,
                              MediaStorageService mediaStorage,
                              FfmpegService ffmpeg,
                              @Value("${streamfolio.ffmpeg.transcode-timeout:PT5M}") Duration transcodeTimeout,
                              @Value("${streamfolio.ffmpeg.hls-segment-time:4}") int hlsSegmentTime,
                              @Value("${streamfolio.ffmpeg.hls-flags:independent_segments+temp_file}") String hlsFlags,
                              @Value("${streamfolio.ffmpeg.hls-playlist-type:vod}") String hlsPlaylistType,
                              @Value("${streamfolio.ffmpeg.thumbnail-count:6}") int thumbnailCount,
                              @Value("${streamfolio.ffmpeg.hardware-acceleration:auto}") String hardwareAcceleration,
                              @Value("${streamfolio.ffmpeg.nvidia-encoder:h264_nvenc}") String nvidiaEncoder,
                              @Value("${streamfolio.ffmpeg.nvidia-preset:fast}") String nvidiaPreset,
                              @Value("${streamfolio.ffmpeg.cpu-preset:veryfast}") String cpuPreset,
                              @Value("${streamfolio.ffmpeg.hls-variants:" + DEFAULT_VARIANTS + "}") String variants) {
        this.catalogService = catalogService;
        this.mediaStorage = mediaStorage;
        this.ffmpeg = ffmpeg;
        this.transcodeTimeout = transcodeTimeout == null ? Duration.ofMinutes(5) : transcodeTimeout;
        this.hlsSegmentTime = Math.max(1, hlsSegmentTime);
        this.hlsFlags = textOrDefault(hlsFlags, "independent_segments+temp_file");
        this.hlsPlaylistType = textOrDefault(hlsPlaylistType, "vod");
        this.thumbnailCount = Math.max(1, thumbnailCount);
        this.hardwareAcceleration = normalizeHardwareAcceleration(hardwareAcceleration);
        this.nvidiaEncoder = textOrDefault(nvidiaEncoder, "h264_nvenc");
        this.nvidiaPreset = textOrDefault(nvidiaPreset, "fast");
        this.cpuPreset = textOrDefault(cpuPreset, "veryfast");
        this.variants = parseVariants(variants);
    }

    public List<String> variantNames() {
        return variants.stream().map(Variant::name).toList();
    }

    public HlsTranscodeResult transcodeToHls(Long videoId) {
        return transcodeToHls(videoId, false);
    }

    public HlsTranscodeResult transcodeToHls(Long videoId, boolean force) {
        return transcodeToHlsAndThumbnails(videoId, force, ProgressReporter.noop(), NEVER_CANCELLED);
    }

    public HlsTranscodeResult transcodeToHlsAndThumbnails(Long videoId, boolean force, ProgressReporter progress) {
        return transcodeToHlsAndThumbnails(videoId, force, progress, NEVER_CANCELLED);
    }

    public HlsTranscodeResult transcodeToHlsAndThumbnails(Long videoId, boolean force, ProgressReporter progress,
                                                          BooleanSupplier cancellationRequested) {
        ensureWritableMediaStorage();
        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = sourcePath(video);
        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        Path playlist = mediaStorage.hlsMasterPlaylist(videoId);
        if (allOutputsReady(videoId) && !force) {
            return new HlsTranscodeResult(videoId, false, source, outputDirectory, playlist, "Sorties HLS et thumbnails deja presentes.");
        }
        for (int index = 0; index < variants.size(); index++) {
            abortIfCancelled(cancellationRequested);
            Variant variant = variants.get(index);
            int baseProgress = 10 + index * 20;
            progress.report(baseProgress, "Generation HLS " + variant.name() + ".");
            transcodeVariant(videoId, variant.name(), force,
                (childProgress, message) -> progress.report(baseProgress + childProgress / 5, message),
                cancellationRequested);
        }
        abortIfCancelled(cancellationRequested);
        progress.report(78, "Generation des thumbnails timeline.");
        generateTimelineThumbnails(videoId, force,
            (childProgress, message) -> progress.report(78 + childProgress / 5, message),
            cancellationRequested);
        progress.report(95, "Validation des sorties media.");
        return completeHls(videoId, force);
    }

    public HlsTranscodeResult transcodeVariant(Long videoId, String variantName, boolean force, ProgressReporter progress) {
        return transcodeVariant(videoId, variantName, force, progress, NEVER_CANCELLED);
    }

    public HlsTranscodeResult transcodeVariant(Long videoId, String variantName, boolean force, ProgressReporter progress,
                                               BooleanSupplier cancellationRequested) {
        ensureWritableMediaStorage();
        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = sourcePath(video);
        Variant variant = variant(variantName);
        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        Path variantDirectory = outputDirectory.resolve(variant.name());
        Path variantPlaylist = variantDirectory.resolve("playlist.m3u8");
        if (Files.isRegularFile(variantPlaylist) && !force) {
            return new HlsTranscodeResult(videoId, false, source, outputDirectory, variantPlaylist, "Variante " + variant.name() + " deja presente.");
        }
        prepareVariantDirectory(variantDirectory, force);
        progress.report(10, variant.name() + " : dossier pret.");
        abortIfCancelled(cancellationRequested);
        runVariantCommand(source, variant, variantPlaylist, progress, cancellationRequested);
        validatePlaylist(variantPlaylist, false);
        progress.report(100, variant.name() + " : HLS genere.");
        return new HlsTranscodeResult(videoId, true, source, outputDirectory, variantPlaylist, "Variante " + variant.name() + " generee.");
    }

    public HlsTranscodeResult generateTimelineThumbnails(Long videoId, boolean force, ProgressReporter progress) {
        return generateTimelineThumbnails(videoId, force, progress, NEVER_CANCELLED);
    }

    public HlsTranscodeResult generateTimelineThumbnails(Long videoId, boolean force, ProgressReporter progress,
                                                         BooleanSupplier cancellationRequested) {
        ensureWritableMediaStorage();
        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = sourcePath(video);
        Path directory = mediaStorage.thumbnailDirectory(videoId);
        Path manifest = mediaStorage.thumbnailManifest(videoId);
        if (Files.isRegularFile(manifest) && !force) {
            return new HlsTranscodeResult(videoId, false, source, directory, manifest, "Thumbnails timeline deja presents.");
        }
        try {
            Files.createDirectories(directory);
            if (force) {
                cleanDirectory(directory);
                Files.createDirectories(directory);
            }
            List<ThumbnailEntry> entries = new ArrayList<>();
            int duration = Math.max(1, video.getDurationSeconds());
            int step = Math.max(1, duration / thumbnailCount);
            for (int index = 0; index < thumbnailCount; index++) {
                abortIfCancelled(cancellationRequested);
                int second = Math.min(Math.max(0, duration - 1), index * step);
                String filename = "thumb_" + String.format("%03d", index) + ".jpg";
                Path output = directory.resolve(filename);
                progress.report(Math.min(95, 10 + index * Math.max(1, 85 / thumbnailCount)), "Thumbnail " + (index + 1) + "/" + thumbnailCount + ".");
                ffmpeg.runOrThrow(thumbnailCommand(source, second, output), transcodeTimeout, cancellationRequested);
                entries.add(new ThumbnailEntry(second, "/api/videos/" + videoId + "/thumbnails/" + filename));
            }
            Files.writeString(manifest, manifestJson(videoId, entries));
            progress.report(100, "Thumbnails timeline generes.");
            return new HlsTranscodeResult(videoId, true, source, directory, manifest, "Thumbnails timeline generes.");
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de generer les thumbnails: " + exception.getMessage());
        }
    }

    public HlsTranscodeResult completeHls(Long videoId, boolean force) {
        ensureWritableMediaStorage();
        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = sourcePath(video);
        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        Path playlist = mediaStorage.hlsMasterPlaylist(videoId);
        for (Variant variant : variants) {
            validatePlaylist(outputDirectory.resolve(variant.name()).resolve("playlist.m3u8"), false);
        }
        Path thumbnailManifest = mediaStorage.thumbnailManifest(videoId);
        if (!Files.isRegularFile(thumbnailManifest) || !Files.isReadable(thumbnailManifest)) {
            throw new BadRequestException("Manifest thumbnails non genere.");
        }
        writeMasterPlaylist(playlist);
        validatePlaylist(playlist, true);
        mediaStorage.publishDerivedMedia(videoId);
        return new HlsTranscodeResult(videoId, force, source, outputDirectory, playlist, "HLS multi-bitrate et thumbnails generes.");
    }

    private void ensureWritableMediaStorage() {
        if (mediaStorage.mode() == MediaStorageMode.CLASSPATH) {
            throw new BadRequestException("Transcodage HLS disponible avec le stockage media local ou MinIO uniquement.");
        }
    }

    private boolean allOutputsReady(Long videoId) {
        if (!mediaStorage.hlsMasterPlaylistExists(videoId) || !mediaStorage.thumbnailManifestExists(videoId)) {
            return false;
        }
        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        return variants.stream()
            .map(variant -> outputDirectory.resolve(variant.name()).resolve("playlist.m3u8"))
            .allMatch(Files::isRegularFile);
    }

    private Path sourcePath(CatalogVideo video) {
        Path source = mediaStorage.stageOriginalForTranscoding(video.getAssetFilename());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new NotFoundException("Fichier source introuvable pour transcodage: " + video.getAssetFilename());
        }
        return source;
    }

    private void prepareVariantDirectory(Path outputDirectory, boolean force) {
        try {
            Files.createDirectories(outputDirectory);
            if (force) {
                cleanDirectory(outputDirectory);
                Files.createDirectories(outputDirectory);
            }
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de preparer la variante HLS: " + exception.getMessage());
        }
    }

    private void runVariantCommand(Path source, Variant variant, Path playlist, ProgressReporter progress,
                                   BooleanSupplier cancellationRequested) {
        if (shouldTryNvidia()) {
            try {
                progress.report(20, variant.name() + " : encodage GPU NVIDIA via " + nvidiaEncoder + ".");
                ffmpeg.runOrThrow(command(source, variant, playlist, true), transcodeTimeout, cancellationRequested);
                return;
            } catch (BadRequestException exception) {
                if ("nvidia".equals(hardwareAcceleration)) {
                    throw exception;
                }
                progress.report(25, variant.name() + " : repli CPU apres echec GPU.");
            }
        }
        progress.report(30, variant.name() + " : encodage CPU libx264.");
        ffmpeg.runOrThrow(command(source, variant, playlist, false), transcodeTimeout, cancellationRequested);
    }

    private boolean shouldTryNvidia() {
        return !"none".equals(hardwareAcceleration) && ffmpeg.hasEncoder(nvidiaEncoder);
    }

    private List<String> command(Path source, Variant variant, Path playlist, boolean nvidia) {
        Path outputDirectory = playlist.getParent();
        List<String> videoCodec = nvidia
            ? List.of("-c:v", nvidiaEncoder, "-preset", nvidiaPreset, "-b:v", variant.videoBitrate())
            : List.of("-c:v", "libx264", "-preset", cpuPreset, "-b:v", variant.videoBitrate());
        List<String> command = new ArrayList<>();
        command.add(ffmpeg.binary());
        command.addAll(List.of("-y", "-i", source.toString(), "-map", "0:v:0", "-map", "0:a:0?", "-vf", scaleFilter(variant)));
        command.addAll(videoCodec);
        command.addAll(List.of(
            "-pix_fmt", "yuv420p", "-flags", "+cgop", "-g", String.valueOf(hlsSegmentTime * 24), "-sc_threshold", "0",
            "-c:a", "aac", "-b:a", "128k", "-ac", "2",
            "-f", "hls", "-hls_time", String.valueOf(hlsSegmentTime), "-hls_list_size", "0", "-hls_playlist_type", hlsPlaylistType,
            "-hls_flags", hlsFlags,
            "-hls_segment_filename", outputDirectory.resolve("segment_%03d.ts").toString(), playlist.toString()
        ));
        return List.copyOf(command);
    }

    private List<String> thumbnailCommand(Path source, int second, Path output) {
        return List.of(ffmpeg.binary(), "-y", "-ss", String.valueOf(second), "-i", source.toString(), "-frames:v", "1", "-q:v", "3", "-vf", "scale=320:-1", output.toString());
    }

    private String scaleFilter(Variant variant) {
        return "scale=w=" + variant.width() + ":h=" + variant.height()
            + ":force_original_aspect_ratio=decrease,pad=w=" + variant.width()
            + ":h=" + variant.height() + ":x=(ow-iw)/2:y=(oh-ih)/2:color=black";
    }

    private void writeMasterPlaylist(Path playlist) {
        StringBuilder content = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
        if (hlsFlags.contains("independent_segments")) {
            content.append("#EXT-X-INDEPENDENT-SEGMENTS\n");
        }
        for (Variant variant : variants) {
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(variant.bandwidth()).append(",RESOLUTION=")
                .append(variant.width()).append('x').append(variant.height()).append(",NAME=\"").append(variant.name()).append("\"\n")
                .append(variant.name()).append("/playlist.m3u8\n");
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
                throw new BadRequestException("Playlist HLS non generee: " + playlist.getFileName());
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

    private Variant variant(String name) {
        return variants.stream()
            .filter(variant -> variant.name().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Variante HLS inconnue: " + name));
    }

    private List<Variant> parseVariants(String value) {
        String raw = textOrDefault(value, DEFAULT_VARIANTS);
        List<Variant> parsed = new ArrayList<>();
        for (String item : raw.split(",")) {
            String[] parts = item.trim().split(":");
            if (parts.length != 5) {
                throw new BadRequestException("Variante HLS invalide: " + item);
            }
            try {
                parsed.add(new Variant(parts[0].trim(), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()), parts[3].trim(), Integer.parseInt(parts[4].trim())));
            } catch (NumberFormatException exception) {
                throw new BadRequestException("Variante HLS invalide: " + item);
            }
        }
        if (parsed.isEmpty()) {
            throw new BadRequestException("Au moins une variante HLS doit etre configuree.");
        }
        return List.copyOf(parsed);
    }

    private String normalizeHardwareAcceleration(String value) {
        String normalized = value == null || value.isBlank() ? "auto" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "auto", "nvidia", "none" -> normalized;
            default -> "auto";
        };
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void abortIfCancelled(BooleanSupplier cancellationRequested) {
        if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
            throw new TranscodeCancelledException("Transcodage annule par l'utilisateur.");
        }
    }

    private String manifestJson(Long videoId, List<ThumbnailEntry> entries) {
        StringBuilder builder = new StringBuilder("{\n  \"videoId\": ").append(videoId).append(",\n  \"items\": [\n");
        for (int index = 0; index < entries.size(); index++) {
            ThumbnailEntry entry = entries.get(index);
            builder.append("    { \"timeSeconds\": ").append(entry.timeSeconds()).append(", \"url\": \"").append(entry.url()).append("\" }");
            if (index < entries.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        return builder.append("  ]\n}\n").toString();
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

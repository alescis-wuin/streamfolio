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
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranscodingService {
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
        if (mediaStorage.mode() != MediaStorageMode.LOCAL) {
            throw new BadRequestException("Transcodage HLS disponible uniquement avec le profil local-media.");
        }

        CatalogVideo video = catalogService.findVideo(videoId);
        Path source = mediaStorage.localOriginalPath(video.getAssetFilename());
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new NotFoundException("Fichier source introuvable pour transcodage: " + video.getAssetFilename());
        }

        Path outputDirectory = mediaStorage.hlsDirectory(videoId);
        Path playlist = mediaStorage.hlsMasterPlaylist(videoId);
        if (Files.isRegularFile(playlist) && !force) {
            return new HlsTranscodeResult(videoId, false, source, outputDirectory, playlist, "Playlist HLS déjà présente.");
        }

        try {
            Files.createDirectories(outputDirectory);
            if (force) {
                cleanDirectory(outputDirectory);
                Files.createDirectories(outputDirectory);
            }
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de préparer le dossier HLS: " + exception.getMessage());
        }

        List<String> command = command(source, outputDirectory, playlist);
        ffmpeg.runOrThrow(command, transcodeTimeout);
        validatePlaylist(playlist);
        return new HlsTranscodeResult(videoId, true, source, outputDirectory, playlist, "Playlist HLS générée.");
    }

    private List<String> command(Path source, Path outputDirectory, Path playlist) {
        return List.of(
            ffmpeg.binary(),
            "-y",
            "-i", source.toString(),
            "-map", "0:v:0",
            "-map", "0:a:0?",
            "-c:v", "libx264",
            "-preset", "veryfast",
            "-crf", "23",
            "-pix_fmt", "yuv420p",
            "-flags", "+cgop",
            "-g", String.valueOf(hlsSegmentTime * 24),
            "-sc_threshold", "0",
            "-c:a", "aac",
            "-b:a", "128k",
            "-ac", "2",
            "-f", "hls",
            "-hls_time", String.valueOf(hlsSegmentTime),
            "-hls_list_size", "0",
            "-hls_playlist_type", "vod",
            "-hls_segment_filename", outputDirectory.resolve("segment_%03d.ts").toString(),
            playlist.toString()
        );
    }

    private void validatePlaylist(Path playlist) {
        try {
            if (!Files.isRegularFile(playlist) || !Files.isReadable(playlist)) {
                throw new BadRequestException("Playlist HLS non générée.");
            }
            String content = Files.readString(playlist);
            if (!content.contains("#EXTM3U")) {
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
}

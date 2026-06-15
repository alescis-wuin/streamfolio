package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.common.BadRequestException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageService {
    private static final String CLASSPATH_MEDIA_PREFIX = "media/";
    private static final String LOCAL_ORIGINALS_DIR = "originals";
    private static final String LOCAL_SUBTITLES_DIR = "subtitles";
    private static final String LOCAL_HLS_DIR = "hls";
    private static final String LOCAL_THUMBNAILS_DIR = "thumbnails";
    private static final String HLS_MASTER_PLAYLIST = "master.m3u8";
    private static final String THUMBNAIL_MANIFEST = "manifest.json";

    private final MediaStorageMode mode;
    private final Path root;

    public MediaStorageService(@Value("${streamfolio.media.storage:classpath}") String storage,
                               @Value("${streamfolio.media.root:./data/media}") String root) {
        this.mode = MediaStorageMode.from(storage);
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public Resource video(String filename) {
        return switch (mode) {
            case CLASSPATH -> classpath(filename);
            case LOCAL -> new FileSystemResource(localOriginalPath(filename));
        };
    }

    public Resource subtitles(String filename) {
        return switch (mode) {
            case CLASSPATH -> classpath(filename);
            case LOCAL -> new FileSystemResource(localSubtitlePath(filename));
        };
    }

    public Resource hlsPlaylist(Long videoId) {
        return new FileSystemResource(hlsMasterPlaylist(videoId));
    }

    public Resource hlsSegment(Long videoId, String filename) {
        return new FileSystemResource(hlsFile(videoId, filename));
    }

    public Resource thumbnail(Long videoId, String filename) {
        return new FileSystemResource(thumbnailFile(videoId, filename));
    }

    public Path localOriginalPath(String filename) {
        return localPath(LOCAL_ORIGINALS_DIR, filename);
    }

    public Path localSubtitlePath(String filename) {
        return localPath(LOCAL_SUBTITLES_DIR, filename);
    }

    public Path hlsDirectory(Long videoId) {
        return videoScopedDirectory(videoId, LOCAL_HLS_DIR, "Chemin HLS invalide.");
    }

    public Path hlsMasterPlaylist(Long videoId) {
        return hlsDirectory(videoId).resolve(HLS_MASTER_PLAYLIST);
    }

    public boolean hlsMasterPlaylistExists(Long videoId) {
        return hlsPlaylist(videoId).exists() && hlsPlaylist(videoId).isReadable();
    }

    public Path thumbnailDirectory(Long videoId) {
        return videoScopedDirectory(videoId, LOCAL_THUMBNAILS_DIR, "Chemin thumbnail invalide.");
    }

    public Path thumbnailManifest(Long videoId) {
        return thumbnailDirectory(videoId).resolve(THUMBNAIL_MANIFEST);
    }

    public boolean thumbnailManifestExists(Long videoId) {
        return thumbnail(videoId, THUMBNAIL_MANIFEST).exists() && thumbnail(videoId, THUMBNAIL_MANIFEST).isReadable();
    }

    public MediaStorageMode mode() {
        return mode;
    }

    public Path root() {
        return root;
    }

    private Resource classpath(String filename) {
        return new ClassPathResource(CLASSPATH_MEDIA_PREFIX + safeFilename(filename));
    }

    private Path localPath(String directory, String filename) {
        Path directoryPath = root.resolve(directory).normalize();
        Path mediaPath = directoryPath.resolve(safeFilename(filename)).normalize();
        if (!mediaPath.startsWith(directoryPath)) {
            throw new BadRequestException("Chemin média invalide.");
        }
        return mediaPath;
    }

    private Path hlsFile(Long videoId, String filename) {
        Path directory = hlsDirectory(videoId);
        Path file = directory.resolve(safeNestedPath(filename, ".ts", ".m3u8", "Chemin HLS invalide.", "Extension HLS invalide.")).normalize();
        if (!file.startsWith(directory)) {
            throw new BadRequestException("Chemin HLS invalide.");
        }
        return file;
    }

    private Path thumbnailFile(Long videoId, String filename) {
        Path directory = thumbnailDirectory(videoId);
        Path file = directory.resolve(safeNestedPath(filename, ".jpg", ".json", "Chemin thumbnail invalide.", "Extension thumbnail invalide.")).normalize();
        if (!file.startsWith(directory)) {
            throw new BadRequestException("Chemin thumbnail invalide.");
        }
        return file;
    }

    private Path videoScopedDirectory(Long videoId, String directory, String message) {
        if (videoId == null || videoId <= 0) {
            throw new BadRequestException("Identifiant vidéo invalide.");
        }
        Path scopedRoot = root.resolve(directory).normalize();
        Path target = scopedRoot.resolve(videoId.toString()).normalize();
        if (!target.startsWith(scopedRoot)) {
            throw new BadRequestException(message);
        }
        return target;
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Nom de fichier média manquant.");
        }
        String value = filename.trim();
        if (value.contains("/") || value.contains("\\") || value.contains("..")) {
            throw new BadRequestException("Nom de fichier média invalide.");
        }
        return value;
    }

    private Path safeNestedPath(String filename, String extensionA, String extensionB, String pathMessage, String extensionMessage) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Nom de fichier manquant.");
        }
        String value = filename.trim();
        if (value.startsWith("/") || value.startsWith("\\") || value.contains("\\") || value.contains("..")) {
            throw new BadRequestException(pathMessage);
        }
        try {
            Path path = Path.of(value).normalize();
            if (path.isAbsolute() || path.startsWith("..") || path.getFileName() == null) {
                throw new BadRequestException(pathMessage);
            }
            for (Path part : path) {
                if (part.toString().isBlank() || "..".equals(part.toString())) {
                    throw new BadRequestException(pathMessage);
                }
            }
            String filenamePart = path.getFileName().toString();
            if (!filenamePart.endsWith(extensionA) && !filenamePart.endsWith(extensionB)) {
                throw new BadRequestException(extensionMessage);
            }
            return path;
        } catch (InvalidPathException exception) {
            throw new BadRequestException(pathMessage);
        }
    }
}

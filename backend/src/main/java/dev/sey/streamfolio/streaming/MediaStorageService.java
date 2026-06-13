package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.common.BadRequestException;
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
            case LOCAL -> local(LOCAL_ORIGINALS_DIR, filename);
        };
    }

    public Resource subtitles(String filename) {
        return switch (mode) {
            case CLASSPATH -> classpath(filename);
            case LOCAL -> local(LOCAL_SUBTITLES_DIR, filename);
        };
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

    private Resource local(String directory, String filename) {
        Path directoryPath = root.resolve(directory).normalize();
        Path mediaPath = directoryPath.resolve(safeFilename(filename)).normalize();
        if (!mediaPath.startsWith(directoryPath)) {
            throw new BadRequestException("Chemin média invalide.");
        }
        return new FileSystemResource(mediaPath);
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
}

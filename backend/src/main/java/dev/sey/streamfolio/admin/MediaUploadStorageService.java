package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaUploadStorageService {
    private static final Set<String> MEDIA_EXTENSIONS = Set.of(".mp4", ".m4v", ".mov", ".mkv", ".webm");
    private static final Set<String> MEDIA_TYPES = Set.of(
        "video/mp4", "video/quicktime", "video/x-matroska", "video/webm", "application/octet-stream"
    );
    private static final Set<String> SUBTITLE_EXTENSIONS = Set.of(".vtt");
    private static final Set<String> SUBTITLE_TYPES = Set.of("text/vtt", "text/plain", "application/octet-stream");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".svg");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/svg+xml", "application/octet-stream");

    private final Path root;
    private final long maxVideoBytes;
    private final long maxSubtitleBytes;
    private final long maxImageBytes;

    public MediaUploadStorageService(@Value("${streamfolio.media.root:./data/media}") String root,
                                     @Value("${streamfolio.admin.upload.max-video-size:4GB}") DataSize maxVideoSize,
                                     @Value("${streamfolio.admin.upload.max-subtitle-size:2MB}") DataSize maxSubtitleSize,
                                     @Value("${streamfolio.admin.upload.max-image-size:10MB}") DataSize maxImageSize) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxVideoBytes = maxVideoSize.toBytes();
        this.maxSubtitleBytes = maxSubtitleSize.toBytes();
        this.maxImageBytes = maxImageSize.toBytes();
    }

    public StoredMediaFile storeVideo(MultipartFile file) {
        return store(file, "originals", MEDIA_EXTENSIONS, MEDIA_TYPES, maxVideoBytes, null);
    }

    public StoredMediaFile storeSubtitle(MultipartFile file) {
        return store(file, "subtitles", SUBTITLE_EXTENSIONS, SUBTITLE_TYPES, maxSubtitleBytes, null);
    }

    public StoredMediaFile storePoster(MultipartFile file) {
        return store(file, "posters", IMAGE_EXTENSIONS, IMAGE_TYPES, maxImageBytes, "posters");
    }

    public StoredMediaFile storeBackdrop(MultipartFile file) {
        return store(file, "backdrops", IMAGE_EXTENSIONS, IMAGE_TYPES, maxImageBytes, "backdrops");
    }

    public Resource image(String kind, String filename) {
        String directory = switch (kind) {
            case "posters" -> "posters";
            case "backdrops" -> "backdrops";
            default -> throw new BadRequestException("Type d'image invalide.");
        };
        return new FileSystemResource(resolveStoredFile(directory, filename, IMAGE_EXTENSIONS));
    }

    private StoredMediaFile store(MultipartFile file, String directory, Set<String> extensions,
                                  Set<String> contentTypes, long maxBytes, String publicKind) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier manquant.");
        }
        if (file.getSize() <= 0) {
            throw new BadRequestException("Fichier vide.");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Fichier trop volumineux.");
        }
        String originalName = safeOriginalFilename(file.getOriginalFilename());
        String extension = extension(originalName);
        if (!extensions.contains(extension)) {
            throw new BadRequestException("Extension de fichier non autorisee: " + extension + ".");
        }
        String contentType = safeContentType(file.getContentType());
        if (!contentTypes.contains(contentType)) {
            throw new BadRequestException("Type MIME non autorise: " + contentType + ".");
        }

        Path directoryPath = root.resolve(directory).normalize();
        try {
            Files.createDirectories(directoryPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path temp = Files.createTempFile(directoryPath, "upload-", ".tmp");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha = HexFormat.of().formatHex(digest.digest());
            String storedFilename = sha + extension;
            Path target = directoryPath.resolve(storedFilename).normalize();
            if (!target.startsWith(directoryPath)) {
                Files.deleteIfExists(temp);
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            boolean created = !Files.exists(target);
            if (created) {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } else {
                Files.deleteIfExists(temp);
            }
            String publicPath = publicKind == null ? null : "/api/media/images/" + publicKind + "/" + storedFilename;
            return new StoredMediaFile(storedFilename, originalName, sha, contentType, file.getSize(), created, publicPath);
        } catch (NoSuchAlgorithmException exception) {
            throw new BadRequestException("SHA-256 indisponible.");
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de stocker le fichier: " + exception.getMessage());
        }
    }

    private Path resolveStoredFile(String directory, String filename, Set<String> extensions) {
        String safeFilename = safeOriginalFilename(filename);
        String extension = extension(safeFilename);
        if (!extensions.contains(extension)) {
            throw new BadRequestException("Extension de fichier non autorisee: " + extension + ".");
        }
        String stem = safeFilename.substring(0, safeFilename.length() - extension.length());
        if (!stem.matches("[a-f0-9]{64}")) {
            throw new BadRequestException("Nom de fichier stocke invalide.");
        }
        Path directoryPath = root.resolve(directory).normalize();
        Path target = directoryPath.resolve(safeFilename).normalize();
        if (!target.startsWith(directoryPath)) {
            throw new BadRequestException("Chemin de fichier invalide.");
        }
        return target;
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Nom de fichier manquant.");
        }
        String value = filename.trim();
        if (value.contains("/") || value.contains("\\") || value.contains("..") || hasControlCharacter(value)) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        try {
            if (Path.of(value).getFileName() == null) {
                throw new BadRequestException("Nom de fichier invalide.");
            }
        } catch (InvalidPathException exception) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        return value;
    }

    private boolean hasControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new BadRequestException("Extension de fichier manquante.");
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String safeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }
}

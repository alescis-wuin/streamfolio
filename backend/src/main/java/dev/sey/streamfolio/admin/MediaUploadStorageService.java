package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private static final Set<String> MEDIA_EXTENSIONS = Set.of(
        ".mp4", ".m4v", ".mov", ".qt", ".wmv", ".asf", ".mkv", ".webm", ".avi", ".divx",
        ".flv", ".f4v", ".swf", ".mts", ".m2ts", ".ts", ".m2t", ".mpeg", ".mpg", ".mpe",
        ".m1v", ".m2v", ".m2p", ".ps", ".vob", ".ogv", ".ogg", ".3gp", ".3g2", ".mxf",
        ".dv", ".rm", ".rmvb", ".mod", ".tod", ".dat"
    );
    private static final Set<String> MEDIA_TYPES = Set.of(
        "video/mp4",
        "video/x-m4v",
        "video/quicktime",
        "video/x-ms-wmv",
        "video/x-ms-asf",
        "application/vnd.ms-asf",
        "video/x-msvideo",
        "video/avi",
        "video/msvideo",
        "application/x-troff-msvideo",
        "video/x-matroska",
        "application/x-matroska",
        "video/webm",
        "video/x-flv",
        "application/x-flv",
        "video/flv",
        "video/x-f4v",
        "application/f4v",
        "video/mp2t",
        "video/vnd.dlna.mpeg-tts",
        "video/mpeg",
        "video/x-mpeg",
        "video/ogg",
        "application/ogg",
        "video/3gpp",
        "video/3gpp2",
        "application/vnd.apple.mpegurl",
        "application/x-mpegurl",
        "application/x-shockwave-flash",
        "application/vnd.adobe.flash.movie",
        "video/dv",
        "video/x-dv",
        "application/vnd.rn-realmedia",
        "video/vnd.rn-realvideo",
        "application/octet-stream"
    );
    private static final Set<String> SUBTITLE_EXTENSIONS = Set.of(".vtt");
    private static final Set<String> SUBTITLE_TYPES = Set.of("text/vtt", "text/plain", "application/octet-stream");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".svg");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/svg+xml", "application/octet-stream");
    private static final byte[] EMPTY_VTT = "WEBVTT\n\n".getBytes(StandardCharsets.UTF_8);

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

    public StoredMediaFile storeTemporaryVideo(MultipartFile file) {
        return storeTemporary(file, "tmp-probe", MEDIA_EXTENSIONS, MEDIA_TYPES, maxVideoBytes);
    }

    public StoredMediaFile storeSubtitle(MultipartFile file) {
        return store(file, "subtitles", SUBTITLE_EXTENSIONS, SUBTITLE_TYPES, maxSubtitleBytes, null);
    }

    public StoredMediaFile storeOptionalSubtitle(MultipartFile file) {
        if (!isMissing(file)) {
            return storeSubtitle(file);
        }
        return storeGeneratedSubtitle();
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

    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary probe uploads.
        }
    }

    private StoredMediaFile store(MultipartFile file, String directory, Set<String> extensions,
                                  Set<String> contentTypes, long maxBytes, String publicKind) {
        UploadCandidate candidate = validate(file, extensions, contentTypes, maxBytes);
        Path directoryPath = root.resolve(directory).normalize();
        try {
            Files.createDirectories(directoryPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path temp = Files.createTempFile(directoryPath, "upload-", ".tmp");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha = HexFormat.of().formatHex(digest.digest());
            String storedFilename = sha + candidate.extension();
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
            return new StoredMediaFile(
                storedFilename,
                candidate.originalName(),
                sha,
                candidate.contentType(),
                file.getSize(),
                created,
                publicPath,
                target
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new BadRequestException("SHA-256 indisponible.");
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de stocker le fichier: " + exception.getMessage());
        }
    }

    private StoredMediaFile storeTemporary(MultipartFile file, String directory, Set<String> extensions,
                                           Set<String> contentTypes, long maxBytes) {
        UploadCandidate candidate = validate(file, extensions, contentTypes, maxBytes);
        Path directoryPath = root.resolve(directory).normalize();
        try {
            Files.createDirectories(directoryPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path temp = Files.createTempFile(directoryPath, "probe-", candidate.extension());
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha = HexFormat.of().formatHex(digest.digest());
            return new StoredMediaFile(
                temp.getFileName().toString(),
                candidate.originalName(),
                sha,
                candidate.contentType(),
                file.getSize(),
                true,
                null,
                temp
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new BadRequestException("SHA-256 indisponible.");
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de preparer le fichier: " + exception.getMessage());
        }
    }

    private StoredMediaFile storeGeneratedSubtitle() {
        Path directoryPath = root.resolve("subtitles").normalize();
        try {
            Files.createDirectories(directoryPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String sha = HexFormat.of().formatHex(digest.digest(EMPTY_VTT));
            String storedFilename = sha + ".vtt";
            Path target = directoryPath.resolve(storedFilename).normalize();
            if (!target.startsWith(directoryPath)) {
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            boolean created = !Files.exists(target);
            if (created) {
                Files.write(target, EMPTY_VTT);
            }
            return new StoredMediaFile(storedFilename, "generated-empty.vtt", sha, "text/vtt", EMPTY_VTT.length, created, null, target);
        } catch (NoSuchAlgorithmException exception) {
            throw new BadRequestException("SHA-256 indisponible.");
        } catch (IOException exception) {
            throw new BadRequestException("Impossible de preparer les sous-titres: " + exception.getMessage());
        }
    }

    private UploadCandidate validate(MultipartFile file, Set<String> extensions, Set<String> contentTypes, long maxBytes) {
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
        return new UploadCandidate(originalName, extension, contentType);
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

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty() || file.getSize() <= 0;
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

    private record UploadCandidate(String originalName, String extension, String contentType) {
    }
}

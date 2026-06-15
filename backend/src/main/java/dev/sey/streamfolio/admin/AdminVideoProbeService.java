package dev.sey.streamfolio.admin;

import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminVideoProbeService {
    private final MediaUploadStorageService storage;
    private final MediaDurationService mediaProbe;

    public AdminVideoProbeService(MediaUploadStorageService storage, MediaDurationService mediaProbe) {
        this.storage = storage;
        this.mediaProbe = mediaProbe;
    }

    public AdminVideoProbeResponse probe(MultipartFile media) {
        StoredMediaFile stored = storage.storeTemporaryVideo(media);
        try {
            return AdminVideoProbeResponse.from(mediaProbe.probeMetadata(stored.storedPath()));
        } finally {
            cleanup(stored.storedPath());
        }
    }

    public byte[] thumbnail(MultipartFile media, Double timestampSeconds) {
        StoredMediaFile stored = storage.storeTemporaryVideo(media);
        try {
            double timestamp = timestampSeconds == null ? 0 : Math.max(0, timestampSeconds);
            return mediaProbe.extractThumbnail(stored.storedPath(), timestamp);
        } finally {
            cleanup(stored.storedPath());
        }
    }

    private void cleanup(Path path) {
        storage.deleteQuietly(path);
    }
}

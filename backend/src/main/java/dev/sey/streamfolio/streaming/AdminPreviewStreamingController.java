package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/videos/{videoId}/preview")
public class AdminPreviewStreamingController {
    private static final MediaType HLS_PLAYLIST = MediaType.parseMediaType("application/vnd.apple.mpegurl;charset=UTF-8");
    private static final MediaType HLS_SEGMENT = MediaType.parseMediaType("video/mp2t");
    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final MediaType JPEG = MediaType.IMAGE_JPEG;

    private final CatalogService catalogService;
    private final MediaStorageService mediaStorage;

    public AdminPreviewStreamingController(CatalogService catalogService, MediaStorageService mediaStorage) {
        this.catalogService = catalogService;
        this.mediaStorage = mediaStorage;
    }

    @GetMapping("/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long videoId) {
        CatalogVideo video = previewVideo(videoId);
        Resource resource = mediaStorage.video(video.getAssetFilename());
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Fichier vidéo introuvable: " + video.getAssetFilename());
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .cacheControl(CacheControl.noCache())
            .body(resource);
    }

    @GetMapping("/hls/**")
    public ResponseEntity<Resource> hls(@PathVariable Long videoId, HttpServletRequest request) {
        previewVideo(videoId);
        String filename = nestedFilename(videoId, request, "/hls/");
        Resource resource = mediaStorage.hlsSegment(videoId, filename);
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Fichier HLS introuvable: " + filename);
        }
        return ResponseEntity.ok()
            .contentType(hlsMediaType(filename))
            .cacheControl(CacheControl.noCache())
            .body(resource);
    }

    @GetMapping("/thumbnails/**")
    public ResponseEntity<Resource> thumbnails(@PathVariable Long videoId, HttpServletRequest request) {
        previewVideo(videoId);
        String filename = nestedFilename(videoId, request, "/thumbnails/");
        Resource resource = mediaStorage.thumbnail(videoId, filename);
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Thumbnail introuvable: " + filename);
        }
        return ResponseEntity.ok()
            .contentType(thumbnailMediaType(filename))
            .cacheControl(CacheControl.noCache())
            .body(resource);
    }

    @GetMapping(value = "/subtitles", produces = "text/vtt;charset=UTF-8")
    public ResponseEntity<Resource> subtitles(@PathVariable Long videoId) {
        CatalogVideo video = previewVideo(videoId);
        Resource resource = mediaStorage.subtitles(video.getSubtitleFilename());
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Sous-titres introuvables: " + video.getSubtitleFilename());
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/vtt;charset=UTF-8"))
            .cacheControl(CacheControl.noCache())
            .body(resource);
    }

    private CatalogVideo previewVideo(Long videoId) {
        return catalogService.findVideo(videoId);
    }

    private String nestedFilename(Long videoId, HttpServletRequest request, String section) {
        String prefix = "/api/admin/videos/" + videoId + "/preview" + section;
        String uri = request.getRequestURI();
        int index = uri.indexOf(prefix);
        if (index < 0) {
            throw new NotFoundException("Fichier introuvable.");
        }
        String raw = uri.substring(index + prefix.length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private MediaType hlsMediaType(String filename) {
        return filename.endsWith(".m3u8") ? HLS_PLAYLIST : HLS_SEGMENT;
    }

    private MediaType thumbnailMediaType(String filename) {
        return filename.endsWith(".json") ? JSON : JPEG;
    }
}

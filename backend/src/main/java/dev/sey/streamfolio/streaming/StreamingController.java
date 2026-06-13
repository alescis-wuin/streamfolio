package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogVideo;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
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
@RequestMapping("/api/videos")
public class StreamingController {
    private final CatalogService catalogService;

    public StreamingController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{videoId}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long videoId) {
        CatalogVideo video = catalogService.findVideo(videoId);
        Resource resource = new ClassPathResource("media/" + video.getAssetFilename());
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Fichier vidéo introuvable: " + video.getAssetFilename());
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
            .body(resource);
    }

    @GetMapping(value = "/{videoId}/subtitles", produces = "text/vtt;charset=UTF-8")
    public ResponseEntity<Resource> subtitles(@PathVariable Long videoId) {
        CatalogVideo video = catalogService.findVideo(videoId);
        Resource resource = new ClassPathResource("media/" + video.getSubtitleFilename());
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Sous-titres introuvables: " + video.getSubtitleFilename());
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/vtt;charset=UTF-8"))
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
            .body(resource);
    }
}

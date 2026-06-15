package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.common.NotFoundException;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/images")
public class MediaImageController {
    private final MediaUploadStorageService storage;

    public MediaImageController(MediaUploadStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/{kind}/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String kind, @PathVariable String filename) {
        Resource resource = storage.image(kind, filename);
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Image media introuvable: " + filename);
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
            .body(resource);
    }
}

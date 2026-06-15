package dev.sey.streamfolio.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUiController {
    @GetMapping(path = "/js/media-admin.js", produces = "application/javascript")
    public ResponseEntity<String> mediaAdmin() throws IOException {
        String source = new ClassPathResource("static/js/media-admin.js")
            .getContentAsString(StandardCharsets.UTF_8)
            .replace("Choisis un fichier vidéo pour détecter la durée automatiquement.", "00:00");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.valueOf("application/javascript"))
            .body(source);
    }
}

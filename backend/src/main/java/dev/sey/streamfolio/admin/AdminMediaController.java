package dev.sey.streamfolio.admin;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/videos")
public class AdminMediaController {
    private final AdminMediaService adminMedia;
    private final AdminVideoProbeService probes;

    public AdminMediaController(AdminMediaService adminMedia, AdminVideoProbeService probes) {
        this.adminMedia = adminMedia;
        this.probes = probes;
    }

    @GetMapping
    public AdminVideoPageResponse videos(@RequestParam(required = false) String query,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String genre,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return adminMedia.videos(query, type, genre, sort, page, size);
    }

    @GetMapping("/ids")
    public List<Long> videoIds(@RequestParam(required = false) String query,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String genre) {
        return adminMedia.videoIds(query, type, genre);
    }

    @GetMapping("/{videoId}")
    public AdminVideoDto video(@PathVariable Long videoId) {
        return adminMedia.video(videoId);
    }

    @PostMapping(path = "/probe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminVideoProbeResponse probe(@RequestPart MultipartFile media) {
        return probes.probe(media);
    }

    @PostMapping(path = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> thumbnail(@RequestParam(required = false) Double timestampSeconds,
                                            @RequestPart MultipartFile media) {
        byte[] thumbnail = probes.thumbnail(media, timestampSeconds);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumbnail.jpg\"")
            .body(thumbnail);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminVideoDto upload(@RequestParam String title,
                                @RequestParam(required = false) Integer releaseYear,
                                @RequestParam(required = false) String genres,
                                @RequestParam(required = false) String synopsis,
                                @RequestParam(required = false) String tagline,
                                @RequestParam(required = false) String maturityRating,
                                @RequestParam(required = false) Integer runtimeMinutes,
                                @RequestParam(required = false) String videoTitle,
                                @RequestParam(required = false) String label,
                                @RequestParam(required = false) Integer durationSeconds,
                                @RequestParam(required = false) String publicationStatus,
                                @RequestPart MultipartFile media,
                                @RequestPart(required = false) MultipartFile subtitles,
                                @RequestPart(required = false) MultipartFile poster,
                                @RequestPart(required = false) MultipartFile backdrop) {
        return adminMedia.upload(
            title, releaseYear, genres, synopsis, tagline, maturityRating, runtimeMinutes,
            videoTitle, label, durationSeconds, publicationStatus, media, subtitles, poster, backdrop
        );
    }

    @PutMapping("/{videoId}")
    public AdminVideoDto update(@PathVariable Long videoId, @RequestBody AdminVideoUpdateRequest request) {
        return adminMedia.update(videoId, request);
    }

    @PostMapping("/{videoId}/link")
    public AdminVideoDto link(@PathVariable Long videoId, @RequestBody AdminVideoLinkRequest request) {
        return adminMedia.link(videoId, request);
    }

    @PostMapping("/{videoId}/unlink")
    public AdminVideoDto unlink(@PathVariable Long videoId) {
        return adminMedia.unlink(videoId);
    }

    @PutMapping("/{videoId}/order")
    public AdminVideoDto order(@PathVariable Long videoId, @RequestBody(required = false) AdminVideoOrderRequest request) {
        return adminMedia.order(videoId, request);
    }

    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long videoId) {
        adminMedia.delete(videoId);
    }
}

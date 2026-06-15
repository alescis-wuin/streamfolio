package dev.sey.streamfolio.admin;

import org.springframework.http.HttpStatus;
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

    public AdminMediaController(AdminMediaService adminMedia) {
        this.adminMedia = adminMedia;
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

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminVideoDto upload(@RequestParam String title,
                                @RequestParam Integer releaseYear,
                                @RequestParam String genres,
                                @RequestParam String synopsis,
                                @RequestParam(required = false) String tagline,
                                @RequestParam(required = false) String maturityRating,
                                @RequestParam(required = false) Integer runtimeMinutes,
                                @RequestParam(required = false) String videoTitle,
                                @RequestParam(required = false) String label,
                                @RequestParam(required = false) Integer durationSeconds,
                                @RequestPart MultipartFile media,
                                @RequestPart MultipartFile subtitles,
                                @RequestPart MultipartFile poster,
                                @RequestPart MultipartFile backdrop) {
        return adminMedia.upload(
            title, releaseYear, genres, synopsis, tagline, maturityRating, runtimeMinutes,
            videoTitle, label, durationSeconds, media, subtitles, poster, backdrop
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
}

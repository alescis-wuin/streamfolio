package dev.sey.streamfolio.catalog;

import dev.sey.streamfolio.catalog.dto.CatalogPageResponse;
import dev.sey.streamfolio.catalog.dto.PlaybackDto;
import dev.sey.streamfolio.catalog.dto.ProgressDto;
import dev.sey.streamfolio.catalog.dto.ProgressUpdateRequest;
import dev.sey.streamfolio.catalog.dto.SectionsResponse;
import dev.sey.streamfolio.catalog.dto.TitleDetailDto;
import dev.sey.streamfolio.domain.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/sections")
    public SectionsResponse sections(@RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.sections(user);
    }

    @GetMapping("/genres")
    public List<String> genres() {
        return catalogService.genres();
    }

    @GetMapping("/catalog")
    public CatalogPageResponse catalog(@RequestParam(required = false) String query,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String genre,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.catalogPage(query, type, genre, page, size, user);
    }

    @GetMapping("/catalog/{slug}")
    public TitleDetailDto detail(@PathVariable String slug,
                                 @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.detail(slug, user);
    }

    @GetMapping("/videos/{videoId}")
    public PlaybackDto playback(@PathVariable Long videoId,
                                @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.playback(videoId, user);
    }

    @PutMapping("/videos/{videoId}/progress")
    public ProgressDto progress(@PathVariable Long videoId,
                                @Valid @RequestBody ProgressUpdateRequest request,
                                @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.updateProgress(videoId, request, user);
    }

    @PostMapping("/titles/{titleId}/watchlist")
    public TitleDetailDto addToWatchlist(@PathVariable Long titleId,
                                         @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.addToWatchlist(titleId, user);
    }

    @DeleteMapping("/titles/{titleId}/watchlist")
    public TitleDetailDto removeFromWatchlist(@PathVariable Long titleId,
                                              @RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return catalogService.removeFromWatchlist(titleId, user);
    }
}

package dev.sey.streamfolio.catalog;

import dev.sey.streamfolio.auth.AuthService;
import dev.sey.streamfolio.catalog.dto.PlaybackDto;
import dev.sey.streamfolio.catalog.dto.ProgressDto;
import dev.sey.streamfolio.catalog.dto.ProgressUpdateRequest;
import dev.sey.streamfolio.catalog.dto.SectionDto;
import dev.sey.streamfolio.catalog.dto.SectionsResponse;
import dev.sey.streamfolio.catalog.dto.TitleCardDto;
import dev.sey.streamfolio.catalog.dto.TitleDetailDto;
import dev.sey.streamfolio.catalog.dto.VideoDto;
import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserProgress;
import dev.sey.streamfolio.domain.WatchlistItem;
import dev.sey.streamfolio.repository.CatalogTitleRepository;
import dev.sey.streamfolio.repository.CatalogVideoRepository;
import dev.sey.streamfolio.repository.UserProgressRepository;
import dev.sey.streamfolio.repository.WatchlistItemRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CatalogTitleRepository titles;
    private final CatalogVideoRepository videos;
    private final UserProgressRepository progressRepository;
    private final WatchlistItemRepository watchlistRepository;
    private final AuthService authService;

    public CatalogService(CatalogTitleRepository titles, CatalogVideoRepository videos,
                          UserProgressRepository progressRepository,
                          WatchlistItemRepository watchlistRepository,
                          AuthService authService) {
        this.titles = titles;
        this.videos = videos;
        this.progressRepository = progressRepository;
        this.watchlistRepository = watchlistRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<String> genres() {
        return titles.findAllByOrderByDisplayPriorityAscTitleAsc().stream()
            .flatMap(title -> title.getGenres().stream())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TitleCardDto> catalog(String query, String type, String genre, UserAccount user) {
        ContentType requestedType = parseType(type);
        String normalizedQuery = normalize(query);
        String normalizedGenre = normalize(genre);

        return titles.findAllByOrderByDisplayPriorityAscTitleAsc().stream()
            .filter(title -> requestedType == null || title.getType() == requestedType)
            .filter(title -> normalizedGenre.isBlank() || title.getGenres().stream().anyMatch(g -> normalize(g).equals(normalizedGenre)))
            .filter(title -> normalizedQuery.isBlank() || matches(title, normalizedQuery))
            .map(title -> toCard(title, user))
            .toList();
    }

    @Transactional(readOnly = true)
    public SectionsResponse sections(UserAccount user) {
        List<TitleCardDto> all = catalog(null, null, null, user);
        TitleCardDto hero = all.isEmpty() ? null : all.get(0);

        List<TitleCardDto> continueWatching = all.stream()
            .filter(item -> item.progress().positionSeconds() > 0 && !item.progress().completed())
            .sorted(Comparator.comparing((TitleCardDto item) -> Optional.ofNullable(item.progress().updatedAt()).orElse(Instant.EPOCH)).reversed())
            .toList();

        List<TitleCardDto> watchlist = all.stream().filter(TitleCardDto::inWatchlist).toList();
        List<TitleCardDto> newReleases = all.stream().filter(item -> item.releaseYear() >= 2025).toList();
        List<TitleCardDto> movies = all.stream().filter(item -> item.type() == ContentType.MOVIE).toList();
        List<TitleCardDto> series = all.stream().filter(item -> item.type() == ContentType.SERIES).toList();

        return new SectionsResponse(hero, List.of(
            new SectionDto("continue", "Continuer", "Reprendre là où la lecture s'est arrêtée.", continueWatching),
            new SectionDto("watchlist", "Ma liste", "Titres conservés pour plus tard.", watchlist),
            new SectionDto("new", "Nouveautés sobres", "Sélection récente pour la démonstration.", newReleases),
            new SectionDto("movies", "Films", "Formats courts et autonomes.", movies),
            new SectionDto("series", "Séries", "Épisodes et progression par vidéo.", series)
        ));
    }

    @Transactional(readOnly = true)
    public TitleDetailDto detail(String slug, UserAccount user) {
        CatalogTitle title = titles.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Titre introuvable: " + slug));
        TitleCardDto card = toCard(title, user);
        List<VideoDto> videoDtos = title.getVideos().stream()
            .map(video -> toVideoDto(video, user))
            .toList();
        return new TitleDetailDto(
            card.id(), card.slug(), card.title(), card.type(), card.releaseYear(), card.maturityRating(),
            card.runtimeMinutes(), card.seasonCount(), card.episodeCount(), card.tagline(), card.synopsis(), card.genres(), card.posterPath(),
            card.backdropPath(), card.inWatchlist(), card.progress(), card.nextVideoId(), videoDtos
        );
    }

    @Transactional(readOnly = true)
    public PlaybackDto playback(Long videoId, UserAccount user) {
        CatalogVideo video = findVideo(videoId);
        CatalogTitle title = video.getTitle();
        return new PlaybackDto(
            video.getId(),
            title.getSlug(),
            title.getTitle(),
            title.getType(),
            video.getVideoTitle(),
            video.getLabel(),
            video.getSeasonNumber(),
            video.getEpisodeNumber(),
            title.getReleaseYear(),
            title.getMaturityRating(),
            List.copyOf(title.getGenres()),
            "/api/videos/" + video.getId() + "/stream",
            "/api/videos/" + video.getId() + "/subtitles",
            progressFor(video, user)
        );
    }

    @Transactional
    public ProgressDto updateProgress(Long videoId, ProgressUpdateRequest request, UserAccount maybeUser) {
        UserAccount user = authService.requireUser(maybeUser);
        CatalogVideo video = findVideo(videoId);
        UserProgress progress = progressRepository.findByUserAndVideo(user, video)
            .orElseGet(() -> new UserProgress(user, video));
        progress.update(request.positionSeconds(), request.durationSeconds());
        return ProgressDto.from(progressRepository.save(progress));
    }

    @Transactional
    public TitleCardDto addToWatchlist(Long titleId, UserAccount maybeUser) {
        UserAccount user = authService.requireUser(maybeUser);
        CatalogTitle title = findTitle(titleId);
        if (!watchlistRepository.existsByUserAndTitle(user, title)) {
            watchlistRepository.save(new WatchlistItem(user, title));
        }
        return toCard(title, user);
    }

    @Transactional
    public TitleCardDto removeFromWatchlist(Long titleId, UserAccount maybeUser) {
        UserAccount user = authService.requireUser(maybeUser);
        CatalogTitle title = findTitle(titleId);
        watchlistRepository.findByUserAndTitle(user, title).ifPresent(watchlistRepository::delete);
        return toCard(title, user);
    }

    @Transactional(readOnly = true)
    public CatalogVideo findVideo(Long videoId) {
        return videos.findById(videoId)
            .orElseThrow(() -> new NotFoundException("Vidéo introuvable: " + videoId));
    }

    private CatalogTitle findTitle(Long titleId) {
        return titles.findById(titleId)
            .orElseThrow(() -> new NotFoundException("Titre introuvable: " + titleId));
    }

    private TitleCardDto toCard(CatalogTitle title, UserAccount user) {
        List<CatalogVideo> titleVideos = title.getVideos();
        ProgressDto aggregateProgress = aggregateProgress(title, user);
        Long nextVideoId = nextVideo(title, user).map(CatalogVideo::getId).orElse(null);
        boolean inWatchlist = user != null && watchlistRepository.existsByUserAndTitle(user, title);
        int seasonCount = title.getType() == ContentType.SERIES
            ? (int) titleVideos.stream().map(CatalogVideo::getSeasonNumber).filter(number -> number > 0).distinct().count()
            : 0;
        int episodeCount = title.getType() == ContentType.SERIES ? titleVideos.size() : 0;

        return new TitleCardDto(
            title.getId(),
            title.getSlug(),
            title.getTitle(),
            title.getType(),
            title.getReleaseYear(),
            title.getMaturityRating(),
            title.getRuntimeMinutes(),
            seasonCount,
            episodeCount,
            title.getTagline(),
            title.getSynopsis(),
            List.copyOf(title.getGenres()),
            title.getPosterPath(),
            title.getBackdropPath(),
            inWatchlist,
            titleVideos.isEmpty() ? ProgressDto.empty(1) : aggregateProgress,
            nextVideoId
        );
    }

    private VideoDto toVideoDto(CatalogVideo video, UserAccount user) {
        return new VideoDto(
            video.getId(),
            video.getSeasonNumber(),
            video.getEpisodeNumber(),
            video.getLabel(),
            video.getVideoTitle(),
            video.getDurationSeconds(),
            "/api/videos/" + video.getId() + "/stream",
            "/api/videos/" + video.getId() + "/subtitles",
            progressFor(video, user)
        );
    }

    private ProgressDto progressFor(CatalogVideo video, UserAccount user) {
        if (user == null) {
            return ProgressDto.empty(video.getDurationSeconds());
        }
        return progressRepository.findByUserAndVideo(user, video)
            .map(ProgressDto::from)
            .orElseGet(() -> ProgressDto.empty(video.getDurationSeconds()));
    }

    private ProgressDto aggregateProgress(CatalogTitle title, UserAccount user) {
        if (title.getVideos().isEmpty()) {
            return ProgressDto.empty(1);
        }
        if (user == null) {
            int totalDuration = title.getVideos().stream().mapToInt(CatalogVideo::getDurationSeconds).sum();
            return ProgressDto.empty(totalDuration);
        }

        Map<Long, UserProgress> byVideo = new HashMap<>();
        for (UserProgress progress : progressRepository.findByUser(user)) {
            byVideo.put(progress.getVideo().getId(), progress);
        }

        int position = 0;
        int duration = 0;
        Instant updatedAt = null;
        for (CatalogVideo video : title.getVideos()) {
            duration += video.getDurationSeconds();
            UserProgress progress = byVideo.get(video.getId());
            if (progress != null) {
                position += progress.getPositionSeconds();
                if (updatedAt == null || progress.getUpdatedAt().isAfter(updatedAt)) {
                    updatedAt = progress.getUpdatedAt();
                }
            }
        }
        return ProgressDto.aggregate(position, duration, updatedAt);
    }

    private Optional<CatalogVideo> nextVideo(CatalogTitle title, UserAccount user) {
        if (title.getVideos().isEmpty()) {
            return Optional.empty();
        }
        if (user == null) {
            return Optional.of(title.getVideos().get(0));
        }
        for (CatalogVideo video : title.getVideos()) {
            ProgressDto progress = progressFor(video, user);
            if (!progress.completed()) {
                return Optional.of(video);
            }
        }
        return Optional.of(title.getVideos().get(0));
    }

    private boolean matches(CatalogTitle title, String query) {
        String source = String.join(" ",
            title.getTitle(),
            title.getTagline(),
            title.getSynopsis(),
            title.getType().name(),
            String.join(" ", title.getGenres())
        );
        return normalize(source).contains(query);
    }

    private ContentType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return ContentType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Type invalide. Valeurs: MOVIE, SERIES.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

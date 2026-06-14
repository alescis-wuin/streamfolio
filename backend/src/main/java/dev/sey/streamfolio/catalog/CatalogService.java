package dev.sey.streamfolio.catalog;

import dev.sey.streamfolio.auth.AuthService;
import dev.sey.streamfolio.catalog.dto.CatalogPageResponse;
import dev.sey.streamfolio.catalog.dto.PlaybackDto;
import dev.sey.streamfolio.catalog.dto.ProgressDto;
import dev.sey.streamfolio.catalog.dto.ProgressUpdateRequest;
import dev.sey.streamfolio.catalog.dto.SectionDto;
import dev.sey.streamfolio.catalog.dto.SectionsResponse;
import dev.sey.streamfolio.catalog.dto.StreamingMode;
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
import dev.sey.streamfolio.streaming.MediaStorageMode;
import dev.sey.streamfolio.streaming.MediaStorageService;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private static final int DEFAULT_PAGE_SIZE = 24;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "displayPriority")
        .and(Sort.by(Sort.Direction.ASC, "title"));

    private final CatalogTitleRepository titles;
    private final CatalogVideoRepository videos;
    private final UserProgressRepository progressRepository;
    private final WatchlistItemRepository watchlistRepository;
    private final AuthService authService;
    private final MediaStorageService mediaStorage;

    public CatalogService(CatalogTitleRepository titles, CatalogVideoRepository videos,
                          UserProgressRepository progressRepository,
                          WatchlistItemRepository watchlistRepository,
                          AuthService authService,
                          MediaStorageService mediaStorage) {
        this.titles = titles;
        this.videos = videos;
        this.progressRepository = progressRepository;
        this.watchlistRepository = watchlistRepository;
        this.authService = authService;
        this.mediaStorage = mediaStorage;
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
    public CatalogPageResponse catalogPage(String query, String type, String genre, Integer page, Integer size, UserAccount user) {
        ContentType requestedType = parseType(type);
        Specification<CatalogTitle> spec = CatalogTitleSpecifications.filtered(query, requestedType, genre);
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size), DEFAULT_SORT);
        UserCatalogContext context = contextFor(user);
        Page<TitleCardDto> result = titles.findPageWithGraph(spec, pageable).map(title -> toCard(title, context));
        return CatalogPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<TitleCardDto> catalog(String query, String type, String genre, UserAccount user) {
        ContentType requestedType = parseType(type);
        Specification<CatalogTitle> spec = CatalogTitleSpecifications.filtered(query, requestedType, genre);
        UserCatalogContext context = contextFor(user);
        return titles.findAll(spec, DEFAULT_SORT).stream()
            .map(title -> toCard(title, context))
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
        UserCatalogContext context = contextFor(user);
        TitleCardDto card = toCard(title, context);
        List<VideoDto> videoDtos = uniqueVideos(title).stream()
            .map(video -> toVideoDto(video, context))
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
        UserCatalogContext context = contextFor(user);
        StreamingMode streamingMode = streamingMode(video.getId());
        String hlsUrl = streamingMode == StreamingMode.HLS_AVAILABLE ? "/api/videos/" + video.getId() + "/hls/master.m3u8" : null;
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
            hlsUrl,
            streamingMode,
            "/api/videos/" + video.getId() + "/subtitles",
            progressFor(video, context)
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
        return toCard(title, contextFor(user));
    }

    @Transactional
    public TitleCardDto removeFromWatchlist(Long titleId, UserAccount maybeUser) {
        UserAccount user = authService.requireUser(maybeUser);
        CatalogTitle title = findTitle(titleId);
        watchlistRepository.findByUserAndTitle(user, title).ifPresent(watchlistRepository::delete);
        return toCard(title, contextFor(user));
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

    private UserCatalogContext contextFor(UserAccount user) {
        if (user == null) {
            return UserCatalogContext.anonymous();
        }

        Map<Long, UserProgress> progressByVideoId = progressRepository.findByUser(user).stream()
            .collect(Collectors.toMap(progress -> progress.getVideo().getId(), Function.identity(), (first, second) -> second));

        Set<Long> watchlistTitleIds = watchlistRepository.findByUser(user).stream()
            .map(item -> item.getTitle().getId())
            .collect(Collectors.toUnmodifiableSet());

        return new UserCatalogContext(user, progressByVideoId, watchlistTitleIds);
    }

    private TitleCardDto toCard(CatalogTitle title, UserCatalogContext context) {
        List<CatalogVideo> titleVideos = uniqueVideos(title);
        ProgressDto aggregateProgress = aggregateProgress(title, context);
        Long nextVideoId = nextVideo(title, context).map(CatalogVideo::getId).orElse(null);
        boolean inWatchlist = context.watchlistTitleIds().contains(title.getId());
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

    private VideoDto toVideoDto(CatalogVideo video, UserCatalogContext context) {
        return new VideoDto(
            video.getId(),
            video.getSeasonNumber(),
            video.getEpisodeNumber(),
            video.getLabel(),
            video.getVideoTitle(),
            video.getDurationSeconds(),
            "/api/videos/" + video.getId() + "/stream",
            "/api/videos/" + video.getId() + "/subtitles",
            progressFor(video, context)
        );
    }

    private ProgressDto progressFor(CatalogVideo video, UserCatalogContext context) {
        UserProgress progress = context.progressByVideoId().get(video.getId());
        return progress == null ? ProgressDto.empty(video.getDurationSeconds()) : ProgressDto.from(progress);
    }

    private ProgressDto aggregateProgress(CatalogTitle title, UserCatalogContext context) {
        List<CatalogVideo> titleVideos = uniqueVideos(title);
        if (titleVideos.isEmpty()) {
            return ProgressDto.empty(1);
        }

        int position = 0;
        int duration = 0;
        Instant updatedAt = null;
        for (CatalogVideo video : titleVideos) {
            duration += video.getDurationSeconds();
            UserProgress progress = context.progressByVideoId().get(video.getId());
            if (progress != null) {
                position += progress.getPositionSeconds();
                if (updatedAt == null || progress.getUpdatedAt().isAfter(updatedAt)) {
                    updatedAt = progress.getUpdatedAt();
                }
            }
        }
        return ProgressDto.aggregate(position, duration, updatedAt);
    }

    private Optional<CatalogVideo> nextVideo(CatalogTitle title, UserCatalogContext context) {
        List<CatalogVideo> titleVideos = uniqueVideos(title);
        if (titleVideos.isEmpty()) {
            return Optional.empty();
        }
        for (CatalogVideo video : titleVideos) {
            ProgressDto progress = progressFor(video, context);
            if (!progress.completed()) {
                return Optional.of(video);
            }
        }
        return Optional.of(titleVideos.get(0));
    }

    private List<CatalogVideo> uniqueVideos(CatalogTitle title) {
        Map<Long, CatalogVideo> byId = new LinkedHashMap<>();
        for (CatalogVideo video : title.getVideos()) {
            byId.putIfAbsent(video.getId(), video);
        }
        return List.copyOf(byId.values());
    }

    private StreamingMode streamingMode(Long videoId) {
        if (mediaStorage.mode() != MediaStorageMode.LOCAL) {
            return StreamingMode.MP4_ONLY;
        }
        return mediaStorage.hlsMasterPlaylistExists(videoId) ? StreamingMode.HLS_AVAILABLE : StreamingMode.HLS_MISSING;
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

    private int safePage(Integer page) {
        int value = page == null ? 0 : page;
        if (value < 0) {
            throw new BadRequestException("Le numéro de page doit être positif ou nul.");
        }
        return value;
    }

    private int safeSize(Integer size) {
        int value = size == null ? DEFAULT_PAGE_SIZE : size;
        if (value < 1 || value > MAX_PAGE_SIZE) {
            throw new BadRequestException("La taille de page doit être comprise entre 1 et " + MAX_PAGE_SIZE + ".");
        }
        return value;
    }

    private record UserCatalogContext(UserAccount user, Map<Long, UserProgress> progressByVideoId, Set<Long> watchlistTitleIds) {
        private static UserCatalogContext anonymous() {
            return new UserCatalogContext(null, Map.of(), Set.of());
        }
    }
}

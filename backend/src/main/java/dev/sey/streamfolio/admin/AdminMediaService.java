package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.common.NotFoundException;
import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.MediaAsset;
import dev.sey.streamfolio.repository.CatalogTitleRepository;
import dev.sey.streamfolio.repository.CatalogVideoRepository;
import dev.sey.streamfolio.repository.MediaAssetRepository;
import dev.sey.streamfolio.repository.TranscodeJobRepository;
import dev.sey.streamfolio.repository.UserProgressRepository;
import dev.sey.streamfolio.repository.WatchlistItemRepository;
import dev.sey.streamfolio.streaming.MediaStorageMode;
import dev.sey.streamfolio.streaming.MediaStorageService;
import dev.sey.streamfolio.transcoding.TranscodeJobService;
import java.text.Normalizer;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminMediaService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_IMAGE_PATH = "/assets/posters-clean/aurora-drift.svg?v6";

    private final CatalogTitleRepository titles;
    private final CatalogVideoRepository videos;
    private final MediaAssetRepository assets;
    private final TranscodeJobRepository transcodeJobs;
    private final UserProgressRepository progress;
    private final WatchlistItemRepository watchlist;
    private final MediaUploadStorageService storage;
    private final MediaDurationService durations;
    private final MediaSourceValidationService sourceValidation;
    private final MediaStorageService mediaStorage;
    private final TranscodeJobService transcodeJobService;
    private final boolean autoTranscode;

    public AdminMediaService(CatalogTitleRepository titles,
                             CatalogVideoRepository videos,
                             MediaAssetRepository assets,
                             TranscodeJobRepository transcodeJobs,
                             UserProgressRepository progress,
                             WatchlistItemRepository watchlist,
                             MediaUploadStorageService storage,
                             MediaDurationService durations,
                             MediaSourceValidationService sourceValidation,
                             MediaStorageService mediaStorage,
                             TranscodeJobService transcodeJobService,
                             @Value("${streamfolio.admin.upload.auto-transcode:true}") boolean autoTranscode) {
        this.titles = titles;
        this.videos = videos;
        this.assets = assets;
        this.transcodeJobs = transcodeJobs;
        this.progress = progress;
        this.watchlist = watchlist;
        this.storage = storage;
        this.durations = durations;
        this.sourceValidation = sourceValidation;
        this.mediaStorage = mediaStorage;
        this.transcodeJobService = transcodeJobService;
        this.autoTranscode = autoTranscode;
    }

    @Transactional(readOnly = true)
    public AdminVideoPageResponse videos(String query, String type, String genre, String sort, Integer page, Integer size) {
        int pageNumber = safePage(page);
        int pageSize = safeSize(size);
        ContentType requestedType = parseType(type);
        Page<CatalogVideo> videoPage = videos.findAll(
            AdminVideoSpecifications.filtered(query, requestedType, genre),
            PageRequest.of(pageNumber, pageSize, sort(sort))
        );
        List<Long> videoIds = videoPage.getContent().stream().map(CatalogVideo::getId).toList();
        Map<Long, MediaAsset> assetsByVideoId = videoIds.isEmpty()
            ? Map.of()
            : assets.findByVideoIdIn(videoIds).stream()
                .collect(Collectors.toMap(asset -> asset.getVideo().getId(), Function.identity(), (first, second) -> first));

        List<AdminVideoDto> items = videoPage.getContent().stream()
            .map(video -> AdminVideoDto.from(video, assetsByVideoId.get(video.getId())))
            .toList();
        return AdminVideoPageResponse.of(items, pageNumber, pageSize, videoPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Long> videoIds(String query, String type, String genre) {
        ContentType requestedType = parseType(type);
        return videos.findAll(
                AdminVideoSpecifications.filtered(query, requestedType, genre),
                Sort.by(Sort.Direction.ASC, "id")
            ).stream()
            .map(CatalogVideo::getId)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminVideoDto video(Long videoId) {
        CatalogVideo video = findVideo(videoId);
        MediaAsset asset = assets.findByVideo(video).orElse(null);
        return AdminVideoDto.from(video, asset);
    }

    @Transactional
    public AdminVideoDto upload(String title,
                                Integer releaseYear,
                                String genres,
                                String synopsis,
                                String tagline,
                                String maturityRating,
                                Integer runtimeMinutes,
                                String videoTitle,
                                String label,
                                Integer durationSeconds,
                                String publicationStatus,
                                MultipartFile media,
                                MultipartFile subtitles,
                                MultipartFile poster,
                                MultipartFile backdrop) {
        String cleanTitle = requiredText(title, "Titre manquant.");
        StoredMediaFile storedMedia = storage.storeVideo(media);
        validateStoredMedia(storedMedia);
        StoredMediaFile storedSubtitles = storage.storeOptionalSubtitle(subtitles);
        StoredMediaFile storedPoster = isMissing(poster) ? null : storage.storePoster(poster);
        StoredMediaFile storedBackdrop = isMissing(backdrop) ? null : storage.storeBackdrop(backdrop);

        int cleanDuration = durationFromUpload(durationSeconds, storedMedia);
        String posterPath = storedPoster == null ? DEFAULT_IMAGE_PATH : storedPoster.publicPath();
        String backdropPath = storedBackdrop == null ? posterPath : storedBackdrop.publicPath();
        CatalogTitle catalogTitle = new CatalogTitle(
            uniqueSlug(cleanTitle, null),
            cleanTitle,
            ContentType.MOVIE,
            safeYear(releaseYear),
            textOrDefault(maturityRating, "Tous publics"),
            positiveOrDefault(runtimeMinutes, Math.max(1, (int) Math.ceil(cleanDuration / 60.0))),
            textOrDefault(tagline, "Nouveau média ajouté depuis l'administration."),
            textOrDefault(synopsis, "Description non renseignée."),
            posterPath,
            backdropPath,
            nextRecommendedPriority()
        );
        parseGenres(genres).forEach(catalogTitle::addGenre);
        CatalogVideo video = new CatalogVideo(
            0,
            0,
            textOrDefault(label, "Film"),
            textOrDefault(videoTitle, cleanTitle),
            cleanDuration,
            storedMedia.storedFilename(),
            storedSubtitles.storedFilename()
        );
        video.updatePublicationStatus(safePublicationStatus(publicationStatus, CatalogVideo.STATUS_PUBLISHED));
        catalogTitle.addVideo(video);
        CatalogTitle savedTitle = titles.save(catalogTitle);
        CatalogVideo savedVideo = savedTitle.getVideos().get(0);
        MediaAsset asset = assets.save(new MediaAsset(
            savedVideo,
            storedMedia.storedFilename(),
            storedMedia.originalFilename(),
            storedMedia.contentSha256(),
            storedMedia.contentType(),
            storedMedia.sizeBytes()
        ));
        startTranscodeAfterUpload(savedVideo);
        return AdminVideoDto.from(savedVideo, asset);
    }

    @Transactional
    public AdminVideoDto update(Long videoId, AdminVideoUpdateRequest request) {
        CatalogVideo video = findVideo(videoId);
        CatalogTitle title = video.getTitle();
        String nextTitle = textOrDefault(request.title(), title.getTitle());
        int nextDuration = positiveOrDefault(request.durationSeconds(), video.getDurationSeconds());
        Set<String> nextGenres = request.genres() == null ? new LinkedHashSet<>(title.getGenres()) : cleanGenres(request.genres());
        title.updateMetadata(
            uniqueSlug(nextTitle, title.getId()),
            nextTitle,
            safeYear(valueOrDefault(request.releaseYear(), title.getReleaseYear())),
            textOrDefault(request.maturityRating(), title.getMaturityRating()),
            positiveOrDefault(request.runtimeMinutes(), title.getRuntimeMinutes()),
            textOrDefault(request.tagline(), title.getTagline()),
            textOrDefault(request.synopsis(), title.getSynopsis()),
            title.getPosterPath(),
            title.getBackdropPath(),
            title.getDisplayPriority(),
            nextGenres
        );
        video.updateMetadata(
            nonNegativeOrDefault(request.seasonNumber(), video.getSeasonNumber()),
            nonNegativeOrDefault(request.episodeNumber(), video.getEpisodeNumber()),
            textOrDefault(request.label(), video.getLabel()),
            textOrDefault(request.videoTitle(), video.getVideoTitle()),
            nextDuration
        );
        if (request.publicationStatus() != null) {
            video.updatePublicationStatus(safePublicationStatus(request.publicationStatus(), video.getPublicationStatus()));
        }
        CatalogVideo savedVideo = videos.save(video);
        MediaAsset asset = assets.findByVideo(savedVideo).orElse(null);
        return AdminVideoDto.from(savedVideo, asset);
    }

    @Transactional
    public AdminVideoDto link(Long videoId, AdminVideoLinkRequest request) {
        if (request == null || request.targetTitleId() == null) {
            throw new BadRequestException("Titre cible manquant.");
        }
        CatalogVideo video = findVideo(videoId);
        CatalogTitle oldTitle = video.getTitle();
        CatalogTitle targetTitle = titles.findById(request.targetTitleId())
            .orElseThrow(() -> new NotFoundException("Titre cible introuvable: " + request.targetTitleId()));

        video.setTitle(targetTitle);
        video.updateMetadata(
            nonNegativeOrDefault(request.seasonNumber(), video.getSeasonNumber()),
            nonNegativeOrDefault(request.episodeNumber(), video.getEpisodeNumber()),
            textOrDefault(request.label(), video.getLabel()),
            textOrDefault(request.videoTitle(), video.getVideoTitle()),
            video.getDurationSeconds()
        );
        CatalogVideo savedVideo = videos.saveAndFlush(video);
        refreshType(oldTitle);
        refreshType(targetTitle);
        MediaAsset asset = assets.findByVideo(savedVideo).orElse(null);
        return AdminVideoDto.from(savedVideo, asset);
    }

    @Transactional
    public AdminVideoDto unlink(Long videoId) {
        CatalogVideo video = findVideo(videoId);
        CatalogTitle oldTitle = video.getTitle();
        CatalogTitle standalone = new CatalogTitle(
            uniqueSlug(video.getVideoTitle(), null),
            video.getVideoTitle(),
            ContentType.MOVIE,
            oldTitle.getReleaseYear(),
            oldTitle.getMaturityRating(),
            Math.max(1, video.getDurationSeconds() / 60),
            oldTitle.getTagline(),
            oldTitle.getSynopsis(),
            oldTitle.getPosterPath(),
            oldTitle.getBackdropPath(),
            nextRecommendedPriority()
        );
        oldTitle.getGenres().forEach(standalone::addGenre);
        CatalogTitle savedStandalone = titles.saveAndFlush(standalone);
        video.setTitle(savedStandalone);
        video.updateMetadata(0, 0, "Film", video.getVideoTitle(), video.getDurationSeconds());
        CatalogVideo savedVideo = videos.saveAndFlush(video);
        refreshType(oldTitle);
        MediaAsset asset = assets.findByVideo(savedVideo).orElse(null);
        return AdminVideoDto.from(savedVideo, asset);
    }

    @Transactional
    public AdminVideoDto order(Long videoId, AdminVideoOrderRequest request) {
        CatalogVideo video = findVideo(videoId);
        video.updateMetadata(
            nonNegativeOrDefault(request == null ? null : request.seasonNumber(), video.getSeasonNumber()),
            nonNegativeOrDefault(request == null ? null : request.episodeNumber(), video.getEpisodeNumber()),
            textOrDefault(request == null ? null : request.label(), video.getLabel()),
            textOrDefault(request == null ? null : request.videoTitle(), video.getVideoTitle()),
            video.getDurationSeconds()
        );
        CatalogVideo savedVideo = videos.save(video);
        MediaAsset asset = assets.findByVideo(savedVideo).orElse(null);
        return AdminVideoDto.from(savedVideo, asset);
    }

    @Transactional
    public void delete(Long videoId) {
        CatalogVideo video = findVideo(videoId);
        CatalogTitle title = video.getTitle();

        assets.deleteByVideo(video);
        transcodeJobs.deleteByVideo(video);
        progress.deleteByVideo(video);
        title.getVideos().removeIf(item -> Objects.equals(item.getId(), videoId));
        videos.delete(video);
        videos.flush();

        if (title.getVideos().isEmpty()) {
            watchlist.deleteByTitle(title);
            titles.delete(title);
            return;
        }

        refreshType(title);
    }

    private void validateStoredMedia(StoredMediaFile storedMedia) {
        try {
            sourceValidation.validateVideo(storedMedia);
        } catch (RuntimeException exception) {
            storage.deleteQuietly(storedMedia.storedPath());
            throw exception;
        }
    }

    private void startTranscodeAfterUpload(CatalogVideo video) {
        if (autoTranscode && mediaStorage.mode() != MediaStorageMode.CLASSPATH) {
            transcodeJobService.submit(video.getId(), false);
        }
    }

    private CatalogVideo findVideo(Long videoId) {
        return videos.findById(videoId)
            .orElseThrow(() -> new NotFoundException("Video introuvable: " + videoId));
    }

    private void refreshType(CatalogTitle title) {
        long count = videos.countByTitle(title);
        title.setType(count > 1 ? ContentType.SERIES : ContentType.MOVIE);
        titles.save(title);
    }

    private int nextRecommendedPriority() {
        int currentMin = titles.findAll().stream()
            .mapToInt(CatalogTitle::getDisplayPriority)
            .min()
            .orElse(1);
        return currentMin == Integer.MIN_VALUE ? Integer.MIN_VALUE : currentMin - 1;
    }

    private Sort sort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String field = parts.length == 0 || parts[0].isBlank() ? "title" : parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        String property = switch (field) {
            case "releaseYear" -> "title.releaseYear";
            case "videoTitle" -> "videoTitle";
            case "duration" -> "durationSeconds";
            case "type" -> "title.type";
            case "synopsis" -> "title.synopsis";
            case "publicationStatus" -> "publicationStatus";
            case "genre", "assetStatus" -> "title.title";
            default -> "title.title";
        };
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
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
        int value = valueOrDefault(page, 0);
        if (value < 0) {
            throw new BadRequestException("Le numero de page doit etre positif ou nul.");
        }
        return value;
    }

    private int safeSize(Integer size) {
        int value = valueOrDefault(size, DEFAULT_PAGE_SIZE);
        if (value < 1 || value > MAX_PAGE_SIZE) {
            throw new BadRequestException("La taille de page doit etre comprise entre 1 et " + MAX_PAGE_SIZE + ".");
        }
        return value;
    }

    private int safeYear(Integer year) {
        int value = valueOrDefault(year, Year.now().getValue());
        if (value < 1888 || value > Year.now().getValue() + 2) {
            throw new BadRequestException("Annee de sortie invalide.");
        }
        return value;
    }

    private int durationFromUpload(Integer durationSeconds, StoredMediaFile storedMedia) {
        if (durationSeconds != null) {
            return positiveOrDefault(durationSeconds, 1);
        }
        return durations.detectSeconds(storedMedia.storedPath()).orElse(1);
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private int positiveOrDefault(Integer value, int fallback) {
        int result = valueOrDefault(value, fallback);
        if (result < 1) {
            throw new BadRequestException("Valeur numerique positive attendue.");
        }
        return result;
    }

    private int nonNegativeOrDefault(Integer value, int fallback) {
        int result = valueOrDefault(value, fallback);
        if (result < 0) {
            throw new BadRequestException("Valeur numerique positive ou nulle attendue.");
        }
        return result;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safePublicationStatus(String value, String fallback) {
        String clean = textOrDefault(value, fallback).toUpperCase(Locale.ROOT);
        if (!CatalogVideo.STATUS_DRAFT.equals(clean) && !CatalogVideo.STATUS_PUBLISHED.equals(clean)) {
            throw new BadRequestException("Statut de publication invalide. Valeurs: DRAFT, PUBLISHED.");
        }
        return clean;
    }

    private Set<String> parseGenres(String value) {
        if (value == null || value.isBlank()) {
            return Set.of("Demo");
        }
        return cleanGenres(List.of(value.split(",")));
    }

    private Set<String> cleanGenres(List<String> values) {
        Set<String> result = values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? Set.of("Demo") : result;
    }

    private String uniqueSlug(String title, Long currentTitleId) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 2;
        while (titles.findBySlug(candidate).filter(existing -> !Objects.equals(existing.getId(), currentTitleId)).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "media" : normalized;
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty() || file.getSize() <= 0;
    }
}

package dev.sey.streamfolio.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "catalog_titles")
public class CatalogTitle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(nullable = false)
    private int releaseYear;

    @Column(nullable = false, length = 20)
    private String maturityRating;

    @Column(nullable = false)
    private int runtimeMinutes;

    @Column(nullable = false, length = 240)
    private String tagline;

    @Column(nullable = false, length = 2000)
    private String synopsis;

    @Column(nullable = false, length = 260)
    private String posterPath;

    @Column(nullable = false, length = 260)
    private String backdropPath;

    @Column(nullable = false)
    private int displayPriority;

    @ElementCollection
    @CollectionTable(name = "catalog_title_genres", joinColumns = @JoinColumn(name = "title_id"))
    @Column(name = "genre", nullable = false, length = 80)
    private Set<String> genres = new LinkedHashSet<>();

    @OneToMany(mappedBy = "title", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seasonNumber ASC, episodeNumber ASC, id ASC")
    private List<CatalogVideo> videos = new ArrayList<>();

    protected CatalogTitle() {
    }

    public CatalogTitle(String slug, String title, ContentType type, int releaseYear, String maturityRating,
                        int runtimeMinutes, String tagline, String synopsis, String posterPath,
                        String backdropPath, int displayPriority) {
        this.slug = slug;
        this.title = title;
        this.type = type;
        this.releaseYear = releaseYear;
        this.maturityRating = maturityRating;
        this.runtimeMinutes = runtimeMinutes;
        this.tagline = tagline;
        this.synopsis = synopsis;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.displayPriority = displayPriority;
    }

    public CatalogTitle addGenre(String genre) {
        this.genres.add(genre);
        return this;
    }

    public CatalogTitle addVideo(CatalogVideo video) {
        video.setTitle(this);
        this.videos.add(video);
        return this;
    }

    public void updateMetadata(String slug, String title, int releaseYear, String maturityRating,
                               int runtimeMinutes, String tagline, String synopsis, String posterPath,
                               String backdropPath, int displayPriority, Set<String> genres) {
        this.slug = slug;
        this.title = title;
        this.releaseYear = releaseYear;
        this.maturityRating = maturityRating;
        this.runtimeMinutes = runtimeMinutes;
        this.tagline = tagline;
        this.synopsis = synopsis;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.displayPriority = displayPriority;
        this.genres.clear();
        this.genres.addAll(genres);
    }

    public void updateImages(String posterPath, String backdropPath) {
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

    public void setRuntimeMinutes(int runtimeMinutes) {
        this.runtimeMinutes = runtimeMinutes;
    }

    public void refreshFrom(CatalogTitle source) {
        this.title = source.title;
        this.type = source.type;
        this.releaseYear = source.releaseYear;
        this.maturityRating = source.maturityRating;
        this.runtimeMinutes = source.runtimeMinutes;
        this.tagline = source.tagline;
        this.synopsis = source.synopsis;
        this.posterPath = source.posterPath;
        this.backdropPath = source.backdropPath;
        this.displayPriority = source.displayPriority;
        this.genres.clear();
        this.genres.addAll(source.genres);
        this.videos.clear();
        for (CatalogVideo video : source.videos) {
            this.addVideo(new CatalogVideo(
                video.getSeasonNumber(),
                video.getEpisodeNumber(),
                video.getLabel(),
                video.getVideoTitle(),
                video.getDurationSeconds(),
                video.getAssetFilename(),
                video.getSubtitleFilename()
            ));
        }
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public ContentType getType() {
        return type;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public String getMaturityRating() {
        return maturityRating;
    }

    public int getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public String getTagline() {
        return tagline;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public int getDisplayPriority() {
        return displayPriority;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public List<CatalogVideo> getVideos() {
        return videos;
    }
}

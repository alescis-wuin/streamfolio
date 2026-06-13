package dev.sey.streamfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_videos")
public class CatalogVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private CatalogTitle title;

    @Column(nullable = false)
    private int seasonNumber;

    @Column(nullable = false)
    private int episodeNumber;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 180)
    private String videoTitle;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false, length = 220)
    private String assetFilename;

    @Column(nullable = false, length = 220)
    private String subtitleFilename;

    protected CatalogVideo() {
    }

    public CatalogVideo(int seasonNumber, int episodeNumber, String label, String videoTitle,
                        int durationSeconds, String assetFilename, String subtitleFilename) {
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.label = label;
        this.videoTitle = videoTitle;
        this.durationSeconds = durationSeconds;
        this.assetFilename = assetFilename;
        this.subtitleFilename = subtitleFilename;
    }

    void setTitle(CatalogTitle title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public CatalogTitle getTitle() {
        return title;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getLabel() {
        return label;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getAssetFilename() {
        return assetFilename;
    }

    public String getSubtitleFilename() {
        return subtitleFilename;
    }
}

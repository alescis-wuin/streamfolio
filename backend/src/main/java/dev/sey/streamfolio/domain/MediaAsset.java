package dev.sey.streamfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false, unique = true)
    private CatalogVideo video;

    @Column(nullable = false, length = 220)
    private String originalFilename;

    @Column(length = 220)
    private String hlsMasterPath;

    @Column(length = 220)
    private String thumbnailManifestPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MediaAssetStatus status = MediaAssetStatus.REGISTERED;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected MediaAsset() {
    }

    public MediaAsset(CatalogVideo video) {
        this.video = video;
        this.originalFilename = video.getAssetFilename();
    }

    public void markReady(String hlsMasterPath, String thumbnailManifestPath) {
        this.hlsMasterPath = hlsMasterPath;
        this.thumbnailManifestPath = thumbnailManifestPath;
        this.status = MediaAssetStatus.READY;
        this.updatedAt = Instant.now();
    }

    public void markMissing() {
        this.status = MediaAssetStatus.MISSING;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = MediaAssetStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CatalogVideo getVideo() {
        return video;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getHlsMasterPath() {
        return hlsMasterPath;
    }

    public String getThumbnailManifestPath() {
        return thumbnailManifestPath;
    }

    public MediaAssetStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

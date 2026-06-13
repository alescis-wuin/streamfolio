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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "user_progress",
    uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_video", columnNames = {"user_id", "video_id"})
)
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private CatalogVideo video;

    @Column(nullable = false)
    private int positionSeconds;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserProgress() {
    }

    public UserProgress(UserAccount user, CatalogVideo video) {
        this.user = user;
        this.video = video;
        this.positionSeconds = 0;
        this.durationSeconds = video.getDurationSeconds();
        this.completed = false;
        this.updatedAt = Instant.now();
    }

    public void update(int positionSeconds, int durationSeconds) {
        int safeDuration = Math.max(1, durationSeconds);
        int safePosition = Math.max(0, Math.min(positionSeconds, safeDuration));
        this.positionSeconds = safePosition;
        this.durationSeconds = safeDuration;
        this.completed = safePosition >= safeDuration * 0.90;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public CatalogVideo getVideo() {
        return video;
    }

    public int getPositionSeconds() {
        return positionSeconds;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

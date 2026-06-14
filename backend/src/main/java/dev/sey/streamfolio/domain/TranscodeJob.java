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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "transcode_jobs")
public class TranscodeJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private CatalogVideo video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TranscodeJobStatus status = TranscodeJobStatus.PENDING;

    @Column(nullable = false)
    private int progressPercent;

    @Column(nullable = false)
    private boolean force;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    private Instant startedAt;

    private Instant finishedAt;

    @Column(length = 1200)
    private String message;

    @Column(length = 500)
    private String outputPath;

    protected TranscodeJob() {
    }

    public TranscodeJob(CatalogVideo video, boolean force) {
        this.video = video;
        this.force = force;
        this.message = "Job en attente.";
    }

    public void markRunning(String message) {
        this.status = TranscodeJobStatus.RUNNING;
        this.progressPercent = Math.max(progressPercent, 5);
        this.startedAt = this.startedAt == null ? Instant.now() : this.startedAt;
        this.message = message;
    }

    public void updateProgress(int progressPercent, String message) {
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.message = message;
    }

    public void markDone(String outputPath, String message) {
        this.status = TranscodeJobStatus.DONE;
        this.progressPercent = 100;
        this.outputPath = outputPath;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = TranscodeJobStatus.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CatalogVideo getVideo() {
        return video;
    }

    public TranscodeJobStatus getStatus() {
        return status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public boolean isForce() {
        return force;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getMessage() {
        return message;
    }

    public String getOutputPath() {
        return outputPath;
    }
}

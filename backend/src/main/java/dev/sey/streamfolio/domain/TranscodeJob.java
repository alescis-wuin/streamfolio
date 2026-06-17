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
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "transcode_jobs")
public class TranscodeJob {
    public static final String WORK_ITEM_BATCH = "batch";
    public static final String WORK_ITEM_READY = "ready";
    public static final String WORK_ITEM_THUMBNAILS = "thumbnails";

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

    @Column(nullable = false, length = 48)
    private String workItem = WORK_ITEM_BATCH;

    private Long parentJobId;

    @Column(length = 80)
    private String workerName;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts = 3;

    private Instant nextAttemptAt;

    @Column(nullable = false)
    private boolean cancellationRequested;

    private Instant lastHeartbeatAt;

    @Column(length = 1200)
    private String message;

    @Column(length = 500)
    private String outputPath;

    protected TranscodeJob() {
    }

    public TranscodeJob(CatalogVideo video, boolean force) {
        this(video, force, WORK_ITEM_BATCH, null);
    }

    public TranscodeJob(CatalogVideo video, boolean force, String workItem, Long parentJobId) {
        this.video = video;
        this.force = force;
        this.workItem = cleanWorkItem(workItem);
        this.parentJobId = parentJobId;
        this.nextAttemptAt = requestedAt;
        this.message = "Job " + this.workItem + " en attente.";
    }

    public boolean isRunnableAt(Instant now) {
        return status.isRunnable() && !cancellationRequested && (nextAttemptAt == null || !nextAttemptAt.isAfter(now));
    }

    public boolean isBatch() {
        return WORK_ITEM_BATCH.equals(workItem);
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts && !cancellationRequested;
    }

    public void configureRetries(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void markRunning(String message) {
        markRunning(message, null);
    }

    public void markRunning(String message, String workerName) {
        this.status = TranscodeJobStatus.RUNNING;
        this.progressPercent = Math.max(progressPercent, 5);
        this.attemptCount += 1;
        this.startedAt = Instant.now();
        this.finishedAt = null;
        this.nextAttemptAt = null;
        this.lastHeartbeatAt = Instant.now();
        this.message = message;
        this.workerName = workerName;
    }

    public void heartbeat(String message) {
        this.lastHeartbeatAt = Instant.now();
        if (message != null && !message.isBlank()) {
            this.message = message;
        }
    }

    public void updateProgress(int progressPercent, String message) {
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.message = message;
        this.lastHeartbeatAt = Instant.now();
    }

    public void markQueuedForRetry(Duration delay, String message) {
        this.status = TranscodeJobStatus.RETRYING;
        this.nextAttemptAt = Instant.now().plus(delay == null ? Duration.ZERO : delay);
        this.workerName = null;
        this.lastHeartbeatAt = Instant.now();
        this.message = message;
    }

    public void resetForManualRetry(int maxAttempts) {
        this.status = TranscodeJobStatus.RETRYING;
        this.progressPercent = 0;
        this.attemptCount = 0;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.nextAttemptAt = Instant.now();
        this.startedAt = null;
        this.finishedAt = null;
        this.workerName = null;
        this.cancellationRequested = false;
        this.message = "Relance manuelle du job " + workItem + ".";
    }

    public void requestCancellation() {
        this.cancellationRequested = true;
        if (status == TranscodeJobStatus.PENDING || status == TranscodeJobStatus.RETRYING) {
            markCancelled("Job annule avant execution.");
        } else if (status == TranscodeJobStatus.RUNNING) {
            this.status = TranscodeJobStatus.CANCELLING;
            this.message = "Annulation demandee.";
        }
    }

    public void markDone(String outputPath, String message) {
        this.status = TranscodeJobStatus.DONE;
        this.progressPercent = 100;
        this.outputPath = outputPath;
        this.message = message;
        this.finishedAt = Instant.now();
        this.nextAttemptAt = null;
        this.cancellationRequested = false;
    }

    public void markFailed(String message) {
        this.status = TranscodeJobStatus.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
        this.nextAttemptAt = null;
    }

    public void markCancelled(String message) {
        this.status = TranscodeJobStatus.CANCELLED;
        this.message = message;
        this.finishedAt = Instant.now();
        this.nextAttemptAt = null;
        this.cancellationRequested = true;
    }

    private String cleanWorkItem(String value) {
        return value == null || value.isBlank() ? WORK_ITEM_BATCH : value.trim().toLowerCase();
    }

    public Long getId() { return id; }
    public CatalogVideo getVideo() { return video; }
    public TranscodeJobStatus getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }
    public boolean isForce() { return force; }
    public String getWorkItem() { return workItem; }
    public Long getParentJobId() { return parentJobId; }
    public String getWorkerName() { return workerName; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public boolean isCancellationRequested() { return cancellationRequested; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public String getMessage() { return message; }
    public String getOutputPath() { return outputPath; }
}

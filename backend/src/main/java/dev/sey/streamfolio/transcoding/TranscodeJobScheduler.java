package dev.sey.streamfolio.transcoding;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TranscodeJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(TranscodeJobScheduler.class);

    private final TranscodeJobService jobs;
    private final AtomicBoolean closing = new AtomicBoolean(false);

    public TranscodeJobScheduler(TranscodeJobService jobs) {
        this.jobs = jobs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        if (closing.get()) {
            return;
        }
        int count = jobs.requeueInterruptedJobs();
        if (count > 0) {
            log.info("Requeued {} interrupted transcode job(s) after backend startup.", count);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void stopDispatching() {
        closing.set(true);
    }

    @Scheduled(
        initialDelayString = "${streamfolio.transcoding.scheduler-initial-delay-ms:3000}",
        fixedDelayString = "${streamfolio.transcoding.scheduler-interval-ms:5000}"
    )
    public void dispatchRunnableJobs() {
        if (closing.get()) {
            return;
        }
        try {
            int count = jobs.dispatchRunnableJobs();
            if (count > 0) {
                log.debug("Dispatched {} runnable transcode job(s).", count);
            }
        } catch (DataAccessException exception) {
            if (closing.get()) {
                log.debug("Skipping transcode dispatch during shutdown: {}", exception.getMessage());
                return;
            }
            throw exception;
        }
    }
}

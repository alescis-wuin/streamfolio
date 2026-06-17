package dev.sey.streamfolio.transcoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TranscodeJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(TranscodeJobScheduler.class);

    private final TranscodeJobService jobs;

    public TranscodeJobScheduler(TranscodeJobService jobs) {
        this.jobs = jobs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        int count = jobs.requeueInterruptedJobs();
        if (count > 0) {
            log.info("Requeued {} interrupted transcode job(s) after backend startup.", count);
        }
    }

    @Scheduled(
        initialDelayString = "${streamfolio.transcoding.scheduler-initial-delay-ms:3000}",
        fixedDelayString = "${streamfolio.transcoding.scheduler-interval-ms:5000}"
    )
    public void dispatchRunnableJobs() {
        int count = jobs.dispatchRunnableJobs();
        if (count > 0) {
            log.debug("Dispatched {} runnable transcode job(s).", count);
        }
    }
}

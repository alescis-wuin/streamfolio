package dev.sey.streamfolio.transcoding;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class TranscodeCancellationRegistry {
    private final Set<Long> requestedJobIds = ConcurrentHashMap.newKeySet();

    public void request(Long jobId) {
        if (jobId != null) {
            requestedJobIds.add(jobId);
        }
    }

    public void clear(Long jobId) {
        if (jobId != null) {
            requestedJobIds.remove(jobId);
        }
    }

    public BooleanSupplier token(Long jobId) {
        return () -> jobId != null && requestedJobIds.contains(jobId);
    }
}

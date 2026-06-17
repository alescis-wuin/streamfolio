package dev.sey.streamfolio.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AsyncConfig {
    @Bean(name = "transcodeTaskExecutor")
    ThreadPoolTaskExecutor transcodeTaskExecutor(@Value("${streamfolio.transcoding.workers:3}") int workers,
                                                 @Value("${streamfolio.transcoding.queue-capacity:64}") int queueCapacity,
                                                 @Value("${streamfolio.transcoding.await-termination-seconds:30}") int awaitTerminationSeconds) {
        int poolSize = Math.max(1, workers);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(poolSize, queueCapacity));
        executor.setThreadNamePrefix("transcode-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Math.max(1, awaitTerminationSeconds));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "transcodeTaskScheduler")
    ThreadPoolTaskScheduler transcodeTaskScheduler(@Value("${streamfolio.transcoding.scheduler-workers:1}") int workers,
                                                   @Value("${streamfolio.transcoding.await-termination-seconds:30}") int awaitTerminationSeconds) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, workers));
        scheduler.setThreadNamePrefix("transcode-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(Math.max(1, awaitTerminationSeconds));
        scheduler.initialize();
        return scheduler;
    }
}

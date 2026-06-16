package dev.sey.streamfolio.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    @Bean(name = "transcodeTaskExecutor")
    Executor transcodeTaskExecutor(@Value("${streamfolio.transcoding.workers:3}") int workers) {
        int poolSize = Math.max(1, workers);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(8, poolSize * 4));
        executor.setThreadNamePrefix("transcode-");
        executor.initialize();
        return executor;
    }
}

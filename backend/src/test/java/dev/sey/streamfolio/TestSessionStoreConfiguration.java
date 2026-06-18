package dev.sey.streamfolio;

import dev.sey.streamfolio.auth.InMemorySessionStore;
import dev.sey.streamfolio.auth.SessionStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TestSessionStoreConfiguration {
    @Bean
    SessionStore testSessionStore() {
        return new InMemorySessionStore(Clock.systemUTC());
    }
}

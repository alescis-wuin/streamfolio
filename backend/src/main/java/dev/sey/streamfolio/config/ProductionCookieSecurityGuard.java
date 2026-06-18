package dev.sey.streamfolio.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionCookieSecurityGuard implements ApplicationRunner {
    private final Environment environment;
    private final boolean secureCookie;

    public ProductionCookieSecurityGuard(Environment environment,
                                         @Value("${streamfolio.security.cookie-secure:false}") boolean secureCookie) {
        this.environment = environment;
        this.secureCookie = secureCookie;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (prodProfileActive() && !secureCookie) {
            throw new IllegalStateException("Production profile requires secure session cookies. Set STREAMFOLIO_COOKIE_SECURE=true or remove the override.");
        }
    }

    private boolean prodProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}

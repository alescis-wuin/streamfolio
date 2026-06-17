package dev.sey.streamfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SessionCookieServiceTest {
    @Test
    void sessionCookieCanBeMarkedSecureForHttpsProfile() {
        SessionCookieService cookies = new SessionCookieService("STREAMFOLIO_SESSION", true, "Strict", Duration.ofMinutes(30));

        String header = cookies.sessionCookie("token-value").toString();

        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("SameSite=Strict");
    }

    @Test
    void sessionCookieKeepsSecureDisabledForLocalHttpProfiles() {
        SessionCookieService cookies = new SessionCookieService("STREAMFOLIO_SESSION", false, "Strict", Duration.ofMinutes(30));

        String header = cookies.sessionCookie("token-value").toString();

        assertThat(header).contains("HttpOnly");
        assertThat(header).doesNotContain("Secure");
        assertThat(header).contains("SameSite=Strict");
    }
}

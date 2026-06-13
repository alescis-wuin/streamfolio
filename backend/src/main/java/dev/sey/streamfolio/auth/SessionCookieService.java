package dev.sey.streamfolio.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieService {
    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration sessionTtl;

    public SessionCookieService(@Value("${streamfolio.security.cookie-name:STREAMFOLIO_SESSION}") String cookieName,
                                @Value("${streamfolio.security.cookie-secure:false}") boolean secure,
                                @Value("${streamfolio.security.cookie-same-site:Strict}") String sameSite,
                                @Value("${streamfolio.security.session-ttl:PT30M}") Duration sessionTtl) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.sessionTtl = sessionTtl;
    }

    public ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(sessionTtl)
            .build();
    }

    public ResponseCookie expiredCookie() {
        return ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
    }

    public Optional<String> resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }
}

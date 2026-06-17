package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.common.UnauthorizedException;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessions;
    private final Duration sessionTtl;
    private final Clock clock;

    @Autowired
    public AuthService(UserAccountRepository users,
                       PasswordEncoder passwordEncoder,
                       SessionStore sessions,
                       @Value("${streamfolio.security.session-ttl:PT30M}") Duration sessionTtl) {
        this(users, passwordEncoder, sessions, sessionTtl, Clock.systemUTC());
    }

    AuthService(UserAccountRepository users,
                PasswordEncoder passwordEncoder,
                SessionStore sessions,
                Duration sessionTtl,
                Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.sessionTtl = sessionTtl;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = users.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Identifiants invalides."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Identifiants invalides.");
        }

        String token = generateToken();
        Instant expiresAt = Instant.now(clock).plus(sessionTtl);
        sessions.save(token, user.getId(), expiresAt);
        return new LoginResult(token, expiresAt, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByToken(String token) {
        return sessions.find(token)
            .filter(session -> session.expiresAt().isAfter(Instant.now(clock)))
            .flatMap(session -> users.findById(session.userId()));
    }

    public void logout(String token) {
        sessions.delete(token);
    }

    public UserAccount requireUser(UserAccount user) {
        if (user == null) {
            throw new UnauthorizedException("Connexion requise.");
        }
        return user;
    }

    @Scheduled(
        initialDelayString = "${streamfolio.security.session-cleanup-initial-delay-ms:300000}",
        fixedDelayString = "${streamfolio.security.session-cleanup-interval-ms:300000}"
    )
    public void cleanupExpiredSessions() {
        purgeExpiredSessions();
    }

    int purgeExpiredSessions() {
        return purgeExpiredSessions(Instant.now(clock));
    }

    int purgeExpiredSessions(Instant now) {
        return sessions.purgeExpired(now);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String hashPassword(String rawPassword) {
        return new BCryptPasswordEncoder(12).encode(rawPassword);
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

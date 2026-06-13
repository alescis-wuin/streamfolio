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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Duration sessionTtl;
    private final Clock clock;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    @Autowired
    public AuthService(UserAccountRepository users,
                       PasswordEncoder passwordEncoder,
                       @Value("${streamfolio.security.session-ttl:PT30M}") Duration sessionTtl) {
        this(users, passwordEncoder, sessionTtl, Clock.systemUTC());
    }

    AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, Duration sessionTtl, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
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
        sessions.put(token, new SessionData(user.getId(), expiresAt));
        return new LoginResult(token, expiresAt, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionData session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now(clock))) {
            sessions.remove(token);
            return Optional.empty();
        }
        return users.findById(session.userId());
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    public UserAccount requireUser(UserAccount user) {
        if (user == null) {
            throw new UnauthorizedException("Connexion requise.");
        }
        return user;
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

    private record SessionData(Long userId, Instant expiresAt) {
    }
}

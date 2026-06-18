package dev.sey.streamfolio.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "streamfolio.security.session-store", havingValue = "memory", matchIfMissing = true)
public class LocalSessionStore implements SessionStore {
    private final Clock clock;
    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();

    public LocalSessionStore() {
        this(Clock.systemUTC());
    }

    LocalSessionStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(String token, Long userId, Instant expiresAt) {
        if (token == null || token.isBlank() || userId == null || expiresAt == null || !expiresAt.isAfter(Instant.now(clock))) {
            return;
        }
        sessions.put(token, new StoredSession(userId, expiresAt));
    }

    @Override
    public Optional<StoredSession> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        StoredSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(Instant.now(clock))) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public void delete(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    @Override
    public int purgeExpired(Instant now) {
        int before = sessions.size();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        return before - sessions.size();
    }
}

package dev.sey.streamfolio.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {
    private final Clock clock;
    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();

    public InMemorySessionStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(String token, Long userId, Instant expiresAt) {
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

package dev.sey.streamfolio.auth;

import java.time.Instant;
import java.util.Optional;

public interface SessionStore {
    void save(String token, Long userId, Instant expiresAt);

    Optional<StoredSession> find(String token);

    void delete(String token);

    int purgeExpired(Instant now);

    record StoredSession(Long userId, Instant expiresAt) {
    }
}

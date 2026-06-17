package dev.sey.streamfolio.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSessionStore implements SessionStore {
    private static final String DEFAULT_KEY_PREFIX = "streamfolio:sessions:";
    private static final String PAYLOAD_SEPARATOR = "|";

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final Clock clock;

    public RedisSessionStore(StringRedisTemplate redis,
                             @Value("${streamfolio.security.session-key-prefix:" + DEFAULT_KEY_PREFIX + "}") String keyPrefix) {
        this(redis, keyPrefix, Clock.systemUTC());
    }

    RedisSessionStore(StringRedisTemplate redis, String keyPrefix, Clock clock) {
        this.redis = redis;
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.clock = clock;
    }

    @Override
    public void save(String token, Long userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            delete(token);
            return;
        }

        // Redis TTL is the source of truth for session expiry; the timestamp keeps stale reads defensive.
        redis.opsForValue().set(key(token), encode(userId, expiresAt), ttl);
    }

    @Override
    public Optional<StoredSession> find(String token) {
        if (isBlank(token)) {
            return Optional.empty();
        }

        String key = key(token);
        String payload = redis.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        Optional<StoredSession> session = decode(payload);
        if (session.isEmpty() || !session.get().expiresAt().isAfter(Instant.now(clock))) {
            redis.delete(key);
            return Optional.empty();
        }
        return session;
    }

    @Override
    public void delete(String token) {
        if (!isBlank(token)) {
            redis.delete(key(token));
        }
    }

    @Override
    public int purgeExpired(Instant now) {
        // Redis expires session keys automatically, so no application-side scan is required.
        return 0;
    }

    private String key(String token) {
        return keyPrefix + token;
    }

    private static String encode(Long userId, Instant expiresAt) {
        return userId + PAYLOAD_SEPARATOR + expiresAt.toEpochMilli();
    }

    private static Optional<StoredSession> decode(String payload) {
        int separatorIndex = payload.indexOf(PAYLOAD_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == payload.length() - 1) {
            return Optional.empty();
        }
        try {
            Long userId = Long.parseLong(payload.substring(0, separatorIndex));
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(payload.substring(separatorIndex + 1)));
            return Optional.of(new StoredSession(userId, expiresAt));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String normalizePrefix(String configuredPrefix) {
        String prefix = isBlank(configuredPrefix) ? DEFAULT_KEY_PREFIX : configuredPrefix.trim();
        return prefix.endsWith(":") ? prefix : prefix + ":";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

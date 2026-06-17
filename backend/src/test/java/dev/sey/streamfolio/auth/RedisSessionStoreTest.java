package dev.sey.streamfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisSessionStoreTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private RedisSessionStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        store = new RedisSessionStore(redis, "test:sessions", FIXED_CLOCK);
    }

    @Test
    void saveWritesPayloadWithRedisTtl() {
        Instant expiresAt = Instant.parse("2026-01-01T10:30:00Z");

        store.save("abc", 7L, expiresAt);

        verify(values).set("test:sessions:abc", "7|1767263400000", Duration.ofMinutes(30));
    }

    @Test
    void findReturnsStoredSession() {
        when(values.get("test:sessions:abc")).thenReturn("7|1767263400000");

        Optional<SessionStore.StoredSession> result = store.find("abc");

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(7L);
        assertThat(result.get().expiresAt()).isEqualTo(Instant.parse("2026-01-01T10:30:00Z"));
    }

    @Test
    void findDeletesMalformedSession() {
        when(values.get("test:sessions:abc")).thenReturn("bad-payload");

        assertThat(store.find("abc")).isEmpty();
        verify(redis).delete("test:sessions:abc");
    }

    @Test
    void findDeletesExpiredSession() {
        when(values.get("test:sessions:abc")).thenReturn("7|1767261599000");

        assertThat(store.find("abc")).isEmpty();
        verify(redis).delete("test:sessions:abc");
    }
}

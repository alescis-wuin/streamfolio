package dev.sey.streamfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.common.UnauthorizedException;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.repository.UserAccountRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserAccountRepository users;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        authService = new AuthService(users, passwordEncoder, Duration.ofMinutes(30), Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void loginAcceptsBCryptPasswordAndCreatesSession() {
        UserAccount user = user("alexis@example.dev", "demo1234");
        when(users.findByEmail("alexis@example.dev")).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        LoginResult result = authService.login(new LoginRequest(" Alexis@Example.Dev ", "demo1234"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("alexis@example.dev");
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-01-01T10:30:00Z"));
        assertThat(authService.findByToken(result.token())).contains(user);
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserAccount user = user("alexis@example.dev", "demo1234");
        when(users.findByEmail("alexis@example.dev")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alexis@example.dev", "bad-password")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Identifiants invalides.");
    }

    @Test
    void expiredSessionIsRejected() {
        AuthService shortSessionAuth = new AuthService(
            users,
            passwordEncoder,
            Duration.ZERO,
            Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
        );
        UserAccount user = user("alexis@example.dev", "demo1234");
        when(users.findByEmail("alexis@example.dev")).thenReturn(Optional.of(user));

        LoginResult result = shortSessionAuth.login(new LoginRequest("alexis@example.dev", "demo1234"));

        assertThat(shortSessionAuth.findByToken(result.token())).isEmpty();
    }

    @Test
    void cleanupRemovesExpiredSessions() {
        AuthService shortSessionAuth = new AuthService(
            users,
            passwordEncoder,
            Duration.ZERO,
            Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
        );
        UserAccount user = user("alexis@example.dev", "demo1234");
        when(users.findByEmail("alexis@example.dev")).thenReturn(Optional.of(user));

        LoginResult result = shortSessionAuth.login(new LoginRequest("alexis@example.dev", "demo1234"));

        assertThat(shortSessionAuth.purgeExpiredSessions()).isEqualTo(1);
        assertThat(shortSessionAuth.findByToken(result.token())).isEmpty();
    }

    @Test
    void logoutInvalidatesSession() {
        UserAccount user = user("alexis@example.dev", "demo1234");
        when(users.findByEmail("alexis@example.dev")).thenReturn(Optional.of(user));

        LoginResult result = authService.login(new LoginRequest("alexis@example.dev", "demo1234"));
        authService.logout(result.token());

        assertThat(authService.findByToken(result.token())).isEmpty();
    }

    @Test
    void hashPasswordProducesBCryptHash() {
        String hash = AuthService.hashPassword("demo1234");

        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches("demo1234", hash)).isTrue();
    }

    private UserAccount user(String email, String password) {
        UserAccount user = new UserAccount(email, "Alexis", passwordEncoder.encode(password));
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}

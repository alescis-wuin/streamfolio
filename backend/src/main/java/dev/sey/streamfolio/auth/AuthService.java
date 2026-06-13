package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.common.UnauthorizedException;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = users.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Identifiants invalides."));

        if (!user.getPasswordHash().equals(hashPassword(request.password()))) {
            throw new UnauthorizedException("Identifiants invalides.");
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new LoginResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByToken(String token) {
        Long userId = sessions.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return users.findById(userId);
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

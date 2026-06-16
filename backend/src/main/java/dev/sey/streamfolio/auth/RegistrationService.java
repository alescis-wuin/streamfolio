package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import dev.sey.streamfolio.repository.UserAccountRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private static final String INTERNAL_DOMAIN = "users.streamfolio.local";

    private final UserAccountRepository users;
    private final AuthService authService;

    public RegistrationService(UserAccountRepository users, AuthService authService) {
        this.users = users;
        this.authService = authService;
    }

    @Transactional
    public LoginResult register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = internalEmail(username);
        if (users.findByEmail(email).isPresent()) {
            throw new BadRequestException("Nom d'utilisateur déjà utilisé.");
        }
        users.saveAndFlush(new UserAccount(
            email,
            username,
            AuthService.hashPassword(request.password()),
            Set.of(UserRole.USER)
        ));
        return authService.login(new LoginRequest(email, request.password()));
    }

    public static String internalEmail(String username) {
        return normalizeUsername(username) + "@" + INTERNAL_DOMAIN;
    }

    private static String normalizeUsername(String username) {
        String value = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new BadRequestException("Nom d'utilisateur manquant.");
        }
        return value;
    }
}

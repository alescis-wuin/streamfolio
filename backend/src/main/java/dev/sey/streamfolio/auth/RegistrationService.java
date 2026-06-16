package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import dev.sey.streamfolio.repository.UserAccountRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserAccountRepository users;
    private final AuthService authService;

    public RegistrationService(UserAccountRepository users, AuthService authService) {
        this.users = users;
        this.authService = authService;
    }

    @Transactional
    public LoginResult register(RegisterRequest request) {
        String identifier = request.effectiveIdentifier();
        String accountKey = IdentifierRules.accountEmailFor(identifier);
        if (users.findByEmail(accountKey).isPresent()) {
            throw new BadRequestException("Identifiant déjà utilisé.");
        }
        users.saveAndFlush(new UserAccount(
            accountKey,
            IdentifierRules.displayNameFor(identifier),
            AuthService.hashPassword(request.password()),
            Set.of(UserRole.USER)
        ));
        return authService.login(new LoginRequest(identifier, request.password()));
    }

    public static String internalEmail(String username) {
        return IdentifierRules.internalEmail(username);
    }
}

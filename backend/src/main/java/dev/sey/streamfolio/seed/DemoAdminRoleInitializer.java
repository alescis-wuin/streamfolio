package dev.sey.streamfolio.seed;

import dev.sey.streamfolio.domain.UserRole;
import dev.sey.streamfolio.repository.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAdminRoleInitializer {
    private final UserAccountRepository users;

    public DemoAdminRoleInitializer(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void grantDemoAdminRole() {
        users.findByEmail("alexis@example.dev").ifPresent(user -> {
            if (!user.hasRole(UserRole.ADMIN)) {
                users.save(user.addRole(UserRole.ADMIN));
            }
        });
    }
}

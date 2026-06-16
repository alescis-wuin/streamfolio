package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import java.util.List;

public record UserDto(Long id, String email, String displayName, List<String> roles) {
    public static UserDto from(UserAccount user) {
        List<String> roles = user.getRoles().stream()
            .map(UserRole::name)
            .sorted()
            .toList();
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            roles.isEmpty() ? List.of(UserRole.USER.name()) : roles
        );
    }
}

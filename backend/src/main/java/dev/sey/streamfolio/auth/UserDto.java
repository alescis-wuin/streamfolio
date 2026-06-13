package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.domain.UserAccount;

public record UserDto(Long id, String email, String displayName) {
    public static UserDto from(UserAccount user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName());
    }
}

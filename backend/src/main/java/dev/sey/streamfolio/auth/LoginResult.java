package dev.sey.streamfolio.auth;

import java.time.Instant;

public record LoginResult(String token, Instant expiresAt, UserDto user) {
}

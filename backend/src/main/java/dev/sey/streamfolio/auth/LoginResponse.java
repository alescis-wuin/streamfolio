package dev.sey.streamfolio.auth;

public record LoginResponse(String token, UserDto user) {
}

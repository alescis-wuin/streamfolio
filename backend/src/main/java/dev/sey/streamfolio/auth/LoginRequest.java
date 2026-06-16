package dev.sey.streamfolio.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    String identifier,
    String email,
    @NotBlank String password
) {
    public String effectiveIdentifier() {
        return identifier != null && !identifier.isBlank() ? identifier : email;
    }
}

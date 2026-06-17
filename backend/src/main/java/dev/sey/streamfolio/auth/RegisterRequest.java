package dev.sey.streamfolio.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    String identifier,
    String username,

    @NotBlank
    @Size(min = 8, max = 120)
    String password
) {
    public String effectiveIdentifier() {
        return identifier != null && !identifier.isBlank() ? identifier : username;
    }
}

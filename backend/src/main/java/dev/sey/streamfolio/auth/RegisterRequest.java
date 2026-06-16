package dev.sey.streamfolio.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 40)
    @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Le nom d'utilisateur ne peut contenir que lettres, chiffres, points, tirets et underscores.")
    String username,

    @NotBlank
    @Size(min = 8, max = 120)
    String password
) {
}

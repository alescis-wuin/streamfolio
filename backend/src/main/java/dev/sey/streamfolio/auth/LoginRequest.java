package dev.sey.streamfolio.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    private String identifier;
    private String email;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String identifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String email() {
        return identifier != null && !identifier.isBlank() ? identifier : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String effectiveIdentifier() {
        return email();
    }
}

package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.common.BadRequestException;
import java.util.Locale;
import java.util.regex.Pattern;

final class IdentifierRules {
    private static final String INTERNAL_DOMAIN = "users.streamfolio.local";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 40;
    private static final int MAX_EMAIL_LENGTH = 180;

    private IdentifierRules() {
    }

    static String accountEmailFor(String identifier) {
        String value = cleanIdentifier(identifier);
        if (value.contains("@")) {
            return normalizeEmail(value);
        }
        return internalEmail(value);
    }

    static String displayNameFor(String identifier) {
        String value = cleanIdentifier(identifier);
        if (value.contains("@")) {
            int at = value.indexOf('@');
            return at > 0 ? value.substring(0, at) : value;
        }
        return value;
    }

    static String internalEmail(String username) {
        return normalizeUsername(username) + "@" + INTERNAL_DOMAIN;
    }

    static boolean isInternalUserEmail(String email) {
        return email != null && email.endsWith("@" + INTERNAL_DOMAIN);
    }

    private static String cleanIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.isBlank()) {
            throw new BadRequestException("Identifiant manquant.");
        }
        return value;
    }

    private static String normalizeEmail(String email) {
        String value = email.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new BadRequestException("Adresse e-mail invalide.");
        }
        return value;
    }

    private static String normalizeUsername(String username) {
        String value = username.trim();
        if (value.length() < MIN_USERNAME_LENGTH || value.length() > MAX_USERNAME_LENGTH || !USERNAME_PATTERN.matcher(value).matches()) {
            throw new BadRequestException("Nom d'utilisateur invalide. Utilise uniquement lettres, chiffres et underscores.");
        }
        return value.toLowerCase(Locale.ROOT);
    }
}

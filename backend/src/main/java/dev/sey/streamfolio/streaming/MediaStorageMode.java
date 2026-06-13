package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.common.BadRequestException;
import java.util.Locale;

public enum MediaStorageMode {
    CLASSPATH,
    LOCAL;

    public static MediaStorageMode from(String value) {
        if (value == null || value.isBlank()) {
            return CLASSPATH;
        }
        try {
            return MediaStorageMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Mode de stockage média invalide. Valeurs: classpath, local.");
        }
    }
}

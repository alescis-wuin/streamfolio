package dev.sey.streamfolio.streaming;

import dev.sey.streamfolio.common.BadRequestException;
import java.util.Locale;

public enum MediaStorageMode {
    CLASSPATH,
    LOCAL,
    MINIO;

    public static MediaStorageMode from(String value) {
        if (value == null || value.isBlank()) {
            return CLASSPATH;
        }
        try {
            return MediaStorageMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Mode de stockage media invalide. Valeurs: classpath, local, minio.");
        }
    }
}

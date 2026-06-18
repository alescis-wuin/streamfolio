package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaSourceValidationService {
    private static final byte[] EBML = bytes(0x1A, 0x45, 0xDF, 0xA3);
    private static final byte[] ASF = bytes(0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11, 0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C);

    private final MediaDurationService probes;
    private final boolean enabled;
    private final boolean rejectInvalid;
    private final boolean requireDuration;
    private final boolean requireVideoStream;
    private final Set<String> allowedVideoCodecs;

    public MediaSourceValidationService(MediaDurationService probes,
                                        @Value("${streamfolio.admin.upload.validation.enabled:true}") boolean enabled,
                                        @Value("${streamfolio.admin.upload.validation.reject-invalid:false}") boolean rejectInvalid,
                                        @Value("${streamfolio.admin.upload.validation.require-duration:false}") boolean requireDuration,
                                        @Value("${streamfolio.admin.upload.validation.require-video-stream:false}") boolean requireVideoStream,
                                        @Value("${streamfolio.admin.upload.validation.allowed-video-codecs:h264,hevc,vp8,vp9,av1,mpeg4,mpeg2video,theora,wmv3,msmpeg4v3,mjpeg}") String allowedVideoCodecs) {
        this.probes = probes;
        this.enabled = enabled;
        this.rejectInvalid = rejectInvalid;
        this.requireDuration = requireDuration;
        this.requireVideoStream = requireVideoStream;
        this.allowedVideoCodecs = parseCsv(allowedVideoCodecs);
    }

    public void validateVideo(StoredMediaFile stored) {
        if (!enabled || stored == null) {
            return;
        }
        SignatureResult signature = signature(stored.storedPath(), extension(stored.storedFilename()));
        MediaProbeMetadata metadata = probes.probeMetadata(stored.storedPath());
        if (!signature.valid() && rejectInvalid) {
            throw new BadRequestException("Signature de conteneur video invalide: " + signature.reason());
        }
        if (requireDuration && (metadata.durationSeconds() == null || metadata.durationSeconds() <= 0)) {
            throw new BadRequestException("Duree video invalide ou absente.");
        }
        if (requireVideoStream && !metadata.hasVideoStream()) {
            throw new BadRequestException("Flux video absent ou illisible.");
        }
        String codec = normalize(metadata.videoCodec());
        if (!codec.isBlank() && !allowedVideoCodecs.isEmpty() && !allowedVideoCodecs.contains(codec)) {
            throw new BadRequestException("Codec video non autorise: " + codec + ".");
        }
    }

    private SignatureResult signature(Path path, String extension) {
        byte[] header = header(path, 16);
        if (header.length == 0) {
            return SignatureResult.invalid("fichier vide");
        }
        return switch (extension) {
            case ".mp4", ".m4v", ".mov", ".qt", ".3gp", ".3g2" -> hasFtyp(header) ? SignatureResult.valid() : SignatureResult.invalid("ftyp manquant");
            case ".mkv", ".webm" -> startsWith(header, EBML) ? SignatureResult.valid() : SignatureResult.invalid("EBML manquant");
            case ".avi", ".divx" -> hasRiffType(header, "AVI ") ? SignatureResult.valid() : SignatureResult.invalid("RIFF AVI manquant");
            case ".flv", ".f4v" -> asciiAt(header, 0, "FLV") ? SignatureResult.valid() : SignatureResult.invalid("FLV manquant");
            case ".ogv", ".ogg" -> asciiAt(header, 0, "OggS") ? SignatureResult.valid() : SignatureResult.invalid("OggS manquant");
            case ".wmv", ".asf" -> startsWith(header, ASF) ? SignatureResult.valid() : SignatureResult.invalid("ASF manquant");
            case ".swf" -> hasSwfSignature(header) ? SignatureResult.valid() : SignatureResult.invalid("SWF manquant");
            case ".ts", ".m2ts", ".mts", ".m2t" -> hasTransportStreamSync(header) ? SignatureResult.valid() : SignatureResult.invalid("sync MPEG-TS manquant");
            case ".mpeg", ".mpg", ".mpe", ".m1v", ".m2v", ".m2p", ".ps", ".vob" -> hasMpegStartCode(header) ? SignatureResult.valid() : SignatureResult.invalid("start code MPEG manquant");
            default -> SignatureResult.valid();
        };
    }

    private byte[] header(Path path, int size) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return bytes.length <= size ? bytes : Arrays.copyOf(bytes, size);
        } catch (IOException exception) {
            return new byte[0];
        }
    }

    private boolean hasFtyp(byte[] header) {
        return asciiAt(header, 4, "ftyp");
    }

    private boolean hasRiffType(byte[] header, String type) {
        return asciiAt(header, 0, "RIFF") && asciiAt(header, 8, type);
    }

    private boolean hasSwfSignature(byte[] header) {
        return asciiAt(header, 0, "FWS") || asciiAt(header, 0, "CWS") || asciiAt(header, 0, "ZWS");
    }

    private boolean hasTransportStreamSync(byte[] header) {
        return header.length > 0 && (unsigned(header[0]) == 0x47 || (header.length > 4 && unsigned(header[4]) == 0x47));
    }

    private boolean hasMpegStartCode(byte[] header) {
        return header.length >= 4 && unsigned(header[0]) == 0x00 && unsigned(header[1]) == 0x00 && unsigned(header[2]) == 0x01
            && (unsigned(header[3]) == 0xBA || unsigned(header[3]) == 0xB3);
    }

    private boolean asciiAt(byte[] header, int offset, String text) {
        if (header.length < offset + text.length()) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            if (header[offset + index] != (byte) text.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] header, byte[] expected) {
        if (header.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (header[index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private Set<String> parseCsv(String value) {
        return Arrays.stream(String.valueOf(value == null ? "" : value).split(","))
            .map(this::normalize)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            bytes[index] = (byte) values[index];
        }
        return bytes;
    }

    private record SignatureResult(boolean valid, String reason) {
        static SignatureResult valid() {
            return new SignatureResult(true, "ok");
        }

        static SignatureResult invalid(String reason) {
            return new SignatureResult(false, reason);
        }
    }
}

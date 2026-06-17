package dev.sey.streamfolio.transcoding;

public class TranscodeCancelledException extends RuntimeException {
    public TranscodeCancelledException(String message) {
        super(message);
    }
}

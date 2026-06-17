package dev.sey.streamfolio.domain;

public enum TranscodeJobStatus {
    PENDING,
    RUNNING,
    RETRYING,
    CANCELLING,
    CANCELLED,
    DONE,
    FAILED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == CANCELLED;
    }

    public boolean isRunnable() {
        return this == PENDING || this == RETRYING;
    }
}

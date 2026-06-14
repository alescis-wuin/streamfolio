package dev.sey.streamfolio.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sey.streamfolio.common.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MediaStorageServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void classpathModeResolvesDemoMedia() {
        MediaStorageService storage = new MediaStorageService("classpath", tempDir.toString());

        assertThat(storage.mode()).isEqualTo(MediaStorageMode.CLASSPATH);
        assertThat(storage.video("aurora-drift.mp4").exists()).isTrue();
        assertThat(storage.subtitles("aurora-drift.vtt").exists()).isTrue();
    }

    @Test
    void localModeResolvesExternalMediaDirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("originals"));
        Files.createDirectories(tempDir.resolve("subtitles"));
        Files.writeString(tempDir.resolve("originals/example.mp4"), "demo video");
        Files.writeString(tempDir.resolve("subtitles/example.vtt"), "WEBVTT");

        MediaStorageService storage = new MediaStorageService("local", tempDir.toString());

        assertThat(storage.mode()).isEqualTo(MediaStorageMode.LOCAL);
        assertThat(storage.video("example.mp4").exists()).isTrue();
        assertThat(storage.subtitles("example.vtt").exists()).isTrue();
    }

    @Test
    void localModeResolvesNestedHlsVariantFiles() throws IOException {
        Files.createDirectories(tempDir.resolve("hls/1/720p"));
        Files.writeString(tempDir.resolve("hls/1/720p/playlist.m3u8"), "#EXTM3U");
        Files.writeString(tempDir.resolve("hls/1/720p/segment_000.ts"), "fake segment");

        MediaStorageService storage = new MediaStorageService("local", tempDir.toString());

        assertThat(storage.hlsSegment(1L, "720p/playlist.m3u8").exists()).isTrue();
        assertThat(storage.hlsSegment(1L, "720p/segment_000.ts").exists()).isTrue();
    }

    @Test
    void rejectsUnsafeFilenames() {
        MediaStorageService storage = new MediaStorageService("local", tempDir.toString());

        assertThatThrownBy(() -> storage.video("../secret.mp4"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("invalide");
    }

    @Test
    void rejectsUnsafeHlsPaths() {
        MediaStorageService storage = new MediaStorageService("local", tempDir.toString());

        assertThatThrownBy(() -> storage.hlsSegment(1L, "../master.m3u8"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("invalide");
        assertThatThrownBy(() -> storage.hlsSegment(1L, "720p/segment.txt"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Extension HLS invalide");
    }

    @Test
    void rejectsUnknownStorageMode() {
        assertThatThrownBy(() -> new MediaStorageService("s3", tempDir.toString()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Mode de stockage média invalide");
    }
}

package dev.sey.streamfolio.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class StreamingLocalMediaIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";
    private static final Path MEDIA_ROOT = createTempMediaRoot();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void localMediaProperties(DynamicPropertyRegistry registry) {
        registry.add("streamfolio.media.storage", () -> "local");
        registry.add("streamfolio.media.root", MEDIA_ROOT::toString);
    }

    @BeforeEach
    void setUp() throws IOException {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        prepareLocalMedia();
    }

    @Test
    void playbackExposesHlsAndThumbnailsWhenFilesExist() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streamUrl").value("/api/videos/1/stream"))
            .andExpect(jsonPath("$.hlsUrl").value("/api/videos/1/hls/master.m3u8"))
            .andExpect(jsonPath("$.thumbnailManifestUrl").value("/api/videos/1/thumbnails/manifest.json"))
            .andExpect(jsonPath("$.streamingMode").value("HLS_AVAILABLE"));
    }

    @Test
    void authenticatedUserCanStreamFromLocalMediaStorage() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1/stream")
                .cookie(session)
                .header(HttpHeaders.RANGE, "bytes=0-1023"))
            .andExpect(status().isPartialContent())
            .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("video/mp4")));
    }

    @Test
    void authenticatedUserCanReadHlsMasterVariantPlaylistAndSegment() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1/hls/master.m3u8").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/vnd.apple.mpegurl")));

        mockMvc.perform(get("/api/videos/1/hls/360p/playlist.m3u8").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/vnd.apple.mpegurl")));

        mockMvc.perform(get("/api/videos/1/hls/360p/segment_000.ts").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("video/mp2t")));
    }

    @Test
    void authenticatedUserCanReadThumbnailManifestAndImage() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1/thumbnails/manifest.json").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")));

        mockMvc.perform(get("/api/videos/1/thumbnails/thumb_000.jpg").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("image/jpeg")));
    }

    @Test
    void authenticatedUserCanReadSubtitlesFromLocalMediaStorage() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1/subtitles").cookie(session))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/vtt")));
    }

    @Test
    void missingLocalVideoReturnsNotFound() throws Exception {
        Cookie session = login();
        Files.deleteIfExists(MEDIA_ROOT.resolve("originals/aurora-drift.mp4"));

        mockMvc.perform(get("/api/videos/1/stream").cookie(session))
            .andExpect(status().isNotFound());
    }

    @Test
    void missingHlsPlaylistKeepsMp4FallbackInPlayback() throws Exception {
        Cookie session = login();
        Files.deleteIfExists(MEDIA_ROOT.resolve("hls/1/master.m3u8"));

        mockMvc.perform(get("/api/videos/1").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streamUrl").value("/api/videos/1/stream"))
            .andExpect(jsonPath("$.hlsUrl").doesNotExist())
            .andExpect(jsonPath("$.streamingMode").value("HLS_MISSING"));
    }

    private Cookie login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alexis@example.dev\",\"password\":\"demo1234\"}"))
            .andExpect(status().isOk())
            .andReturn();
        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private static Path createTempMediaRoot() {
        try {
            return Files.createTempDirectory("streamfolio-local-media-test");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create local media test directory", exception);
        }
    }

    private static void prepareLocalMedia() throws IOException {
        Files.createDirectories(MEDIA_ROOT.resolve("originals"));
        Files.createDirectories(MEDIA_ROOT.resolve("subtitles"));
        Files.createDirectories(MEDIA_ROOT.resolve("hls/1/360p"));
        Files.createDirectories(MEDIA_ROOT.resolve("thumbnails/1"));
        copyClasspathMedia("media/aurora-drift.mp4", MEDIA_ROOT.resolve("originals/aurora-drift.mp4"));
        copyClasspathMedia("media/aurora-drift.vtt", MEDIA_ROOT.resolve("subtitles/aurora-drift.vtt"));
        Files.writeString(MEDIA_ROOT.resolve("hls/1/master.m3u8"), "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=640x360,NAME=\"360p\"\n360p/playlist.m3u8\n");
        Files.writeString(MEDIA_ROOT.resolve("hls/1/360p/playlist.m3u8"), "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:1.0,\nsegment_000.ts\n#EXT-X-ENDLIST\n");
        Files.writeString(MEDIA_ROOT.resolve("hls/1/360p/segment_000.ts"), "fake segment");
        Files.writeString(MEDIA_ROOT.resolve("thumbnails/1/manifest.json"), "{\"items\":[{\"timeSeconds\":0,\"url\":\"/api/videos/1/thumbnails/thumb_000.jpg\"}]}");
        Files.writeString(MEDIA_ROOT.resolve("thumbnails/1/thumb_000.jpg"), "fake jpg");
    }

    private static void copyClasspathMedia(String source, Path target) throws IOException {
        try (InputStream input = new ClassPathResource(source).getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

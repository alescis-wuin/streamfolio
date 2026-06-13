package dev.sey.streamfolio.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    private Cookie login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
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
        Files.createDirectories(MEDIA_ROOT.resolve("hls"));
        copyClasspathMedia("media/aurora-drift.mp4", MEDIA_ROOT.resolve("originals/aurora-drift.mp4"));
        copyClasspathMedia("media/aurora-drift.vtt", MEDIA_ROOT.resolve("subtitles/aurora-drift.vtt"));
    }

    private static void copyClasspathMedia(String source, Path target) throws IOException {
        try (InputStream input = new ClassPathResource(source).getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

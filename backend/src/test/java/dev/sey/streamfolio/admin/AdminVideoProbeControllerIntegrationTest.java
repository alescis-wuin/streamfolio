package dev.sey.streamfolio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AdminVideoProbeControllerIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";
    private static final Path MEDIA_ROOT = createTempMediaRoot();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void adminMediaProbeProperties(DynamicPropertyRegistry registry) {
        registry.add("streamfolio.media.storage", () -> "local");
        registry.add("streamfolio.media.root", MEDIA_ROOT::toString);
        registry.add("streamfolio.admin.upload.max-video-size", () -> "20MB");
        registry.add("streamfolio.ffmpeg.probe-binary", () -> "ffprobe-not-installed-for-test");
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(MEDIA_ROOT.resolve("tmp-probe"));
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void probesAcceptedExtendedVideoFormatWithSafeFallbackMetadata() throws Exception {
        Cookie session = login();

        mockMvc.perform(multipart("/api/admin/videos/probe")
                .file(file("media", "archive-demo.webm", "video/webm", "webm media"))
                .with(csrf())
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formattedDuration").value("00:00"))
            .andExpect(jsonPath("$.authors").isArray());
    }

    private MockMultipartFile file(String name, String filename, String contentType, String content) {
        return new MockMultipartFile(name, filename, contentType, content.getBytes());
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
            return Files.createTempDirectory("streamfolio-admin-probe-test");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create admin media probe test directory", exception);
        }
    }
}

package dev.sey.streamfolio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
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

    @Test
    void probesDurationAndExtractsThumbnailFromVendorAvi() throws Exception {
        Assumptions.assumeTrue(binaryAvailable("ffmpeg") && binaryAvailable("ffprobe"), "ffmpeg and ffprobe are required");
        Cookie session = login();
        Path avi = createAviFixture();
        byte[] payload = Files.readAllBytes(avi);

        mockMvc.perform(multipart("/api/admin/videos/probe")
                .file(new MockMultipartFile("media", "fixture.avi", "video/vnd.avi", payload))
                .with(csrf())
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.durationSeconds").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.formattedDuration").value("00:01"));

        MvcResult thumbnail = mockMvc.perform(multipart("/api/admin/videos/thumbnail")
                .file(new MockMultipartFile("media", "fixture.avi", "video/vnd.avi", payload))
                .param("timestampSeconds", "0")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
            .andReturn();
        byte[] image = thumbnail.getResponse().getContentAsByteArray();
        assertThat(image.length).isGreaterThan(3);
        assertThat(image[0]).isEqualTo((byte) 0xFF);
        assertThat(image[1]).isEqualTo((byte) 0xD8);
    }

    private MockMultipartFile file(String name, String filename, String contentType, String content) {
        return new MockMultipartFile(name, filename, contentType, content.getBytes());
    }

    private Path createAviFixture() throws Exception {
        Path output = Files.createTempFile(MEDIA_ROOT, "fixture-", ".avi");
        Process process = new ProcessBuilder(List.of(
            "ffmpeg",
            "-y",
            "-hide_banner",
            "-loglevel", "error",
            "-f", "lavfi",
            "-i", "testsrc=duration=1:size=64x64:rate=1",
            "-c:v", "mpeg4",
            output.toString()
        )).redirectErrorStream(true).start();
        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        Assumptions.assumeTrue(completed && process.exitValue() == 0, "Unable to generate AVI fixture with ffmpeg");
        return output;
    }

    private boolean binaryAvailable(String binary) {
        try {
            Process process = new ProcessBuilder(binary, "-version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
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

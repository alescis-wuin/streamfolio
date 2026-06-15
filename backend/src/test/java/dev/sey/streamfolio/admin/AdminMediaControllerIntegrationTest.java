package dev.sey.streamfolio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminMediaControllerIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";
    private static final Path MEDIA_ROOT = createTempMediaRoot();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void adminMediaProperties(DynamicPropertyRegistry registry) {
        registry.add("streamfolio.media.storage", () -> "local");
        registry.add("streamfolio.media.root", MEDIA_ROOT::toString);
        registry.add("streamfolio.admin.upload.max-video-size", () -> "20MB");
        registry.add("streamfolio.admin.upload.max-image-size", () -> "2MB");
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(MEDIA_ROOT.resolve("originals"));
        Files.createDirectories(MEDIA_ROOT.resolve("subtitles"));
        Files.createDirectories(MEDIA_ROOT.resolve("posters"));
        Files.createDirectories(MEDIA_ROOT.resolve("backdrops"));
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void uploadsVideoAndCreatesRegisteredMediaAssetStoredBySha() throws Exception {
        Cookie session = login();

        JsonNode first = upload(session, "Uploaded Garden", "shared media");
        JsonNode second = upload(session, "Uploaded Garden Copy", "shared media");

        String filename = first.get("assetFilename").asText();
        assertThat(filename).matches("[a-f0-9]{64}\\.mp4");
        assertThat(second.get("assetFilename").asText()).isEqualTo(filename);
        assertThat(Files.exists(MEDIA_ROOT.resolve("originals").resolve(filename))).isTrue();

        mockMvc.perform(get("/api/admin/videos?query=Uploaded&page=0&size=10&sort=title,asc").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagination.totalElements").value(2))
            .andExpect(jsonPath("$.items[0].assetStatus").value("REGISTERED"));
    }

    @Test
    void rejectsInvalidUploadedMediaExtension() throws Exception {
        Cookie session = login();

        mockMvc.perform(multipart("/api/admin/videos")
                .file(file("media", "evil.exe", "application/octet-stream", "bad"))
                .file(file("subtitles", "captions.vtt", "text/vtt", "WEBVTT"))
                .file(file("poster", "poster.jpg", "image/jpeg", "poster"))
                .file(file("backdrop", "backdrop.jpg", "image/jpeg", "backdrop"))
                .param("title", "Invalid Upload")
                .param("releaseYear", "2026")
                .param("genres", "Test")
                .param("synopsis", "Should fail")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(matchesPattern(".*Extension.*")));
    }

    @Test
    void editsLinksOrdersAndUnlinksVideos() throws Exception {
        Cookie session = login();
        JsonNode first = upload(session, "Admin Series Base", "base media");
        JsonNode second = upload(session, "Admin Series Episode", "episode media");
        long firstTitleId = first.get("titleId").asLong();
        long secondVideoId = second.get("videoId").asLong();

        mockMvc.perform(put("/api/admin/videos/" + first.get("videoId").asLong())
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Admin Series Base Updated\",\"genres\":[\"Botanique\",\"Admin\"],\"durationSeconds\":120}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Admin Series Base Updated"))
            .andExpect(jsonPath("$.genres[1]").value("Admin"));

        mockMvc.perform(post("/api/admin/videos/" + secondVideoId + "/link")
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetTitleId\":" + firstTitleId + ",\"seasonNumber\":1,\"episodeNumber\":2,\"label\":\"S1:E2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titleId").value(firstTitleId))
            .andExpect(jsonPath("$.type").value("SERIES"))
            .andExpect(jsonPath("$.episodeNumber").value(2));

        mockMvc.perform(put("/api/admin/videos/" + secondVideoId + "/order")
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonNumber\":1,\"episodeNumber\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.episodeNumber").value(3));

        mockMvc.perform(post("/api/admin/videos/" + secondVideoId + "/unlink")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("MOVIE"))
            .andExpect(jsonPath("$.seasonNumber").value(0));
    }

    private JsonNode upload(Cookie session, String title, String mediaContent) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/admin/videos")
                .file(file("media", "demo.mp4", "video/mp4", mediaContent))
                .file(file("subtitles", "captions.vtt", "text/vtt", "WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nDemo"))
                .file(file("poster", "poster.jpg", "image/jpeg", "poster" + title))
                .file(file("backdrop", "backdrop.jpg", "image/jpeg", "backdrop" + title))
                .param("title", title)
                .param("releaseYear", "2026")
                .param("genres", "Botanique, Admin")
                .param("synopsis", "Description de test admin")
                .param("durationSeconds", "90")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assetStatus").value("REGISTERED"))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
            return Files.createTempDirectory("streamfolio-admin-media-test");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create admin media test directory", exception);
        }
    }
}

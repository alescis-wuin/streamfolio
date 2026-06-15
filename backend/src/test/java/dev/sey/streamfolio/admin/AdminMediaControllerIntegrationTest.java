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

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.repository.CatalogVideoRepository;
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
    private CatalogVideoRepository videos;

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

        UploadedVideo first = upload(session, "Uploaded Garden", "shared media");
        UploadedVideo second = upload(session, "Uploaded Garden Copy", "shared media");

        assertThat(first.assetFilename()).matches("[a-f0-9]{64}\\.mp4");
        assertThat(second.assetFilename()).isEqualTo(first.assetFilename());
        assertThat(Files.exists(MEDIA_ROOT.resolve("originals").resolve(first.assetFilename()))).isTrue();

        mockMvc.perform(get("/api/admin/videos")
                .param("query", first.assetFilename())
                .param("page", "0")
                .param("size", "10")
                .param("sort", "title,asc")
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagination.totalElements").value(2))
            .andExpect(jsonPath("$.items[0].assetStatus").value("REGISTERED"));
    }

    @Test
    void uploadsWithOnlyTitleAndVideoFile() throws Exception {
        Cookie session = login();

        mockMvc.perform(multipart("/api/admin/videos")
                .file(file("media", "minimal.avi", "video/x-msvideo", "avi media"))
                .param("title", "Minimal Upload")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assetStatus").value("REGISTERED"))
            .andExpect(jsonPath("$.title").value("Minimal Upload"));

        CatalogVideo video = videos.findAllWithTitleGraph().stream()
            .filter(item -> "Minimal Upload".equals(item.getTitle().getTitle()))
            .findFirst()
            .orElseThrow();
        assertThat(video.getAssetFilename()).matches("[a-f0-9]{64}\\.avi");
        assertThat(video.getSubtitleFilename()).matches("[a-f0-9]{64}\\.vtt");
        assertThat(Files.exists(MEDIA_ROOT.resolve("subtitles").resolve(video.getSubtitleFilename()))).isTrue();
    }

    @Test
    void uploadsAdditionalVideoFormatWithoutManualDurationField() throws Exception {
        Cookie session = login();

        mockMvc.perform(multipart("/api/admin/videos")
                .file(file("media", "demo.mkv", "video/x-matroska", "matroska media"))
                .file(file("subtitles", "captions.vtt", "text/vtt", "WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nDemo"))
                .file(file("poster", "poster.jpg", "image/jpeg", "poster mkv"))
                .file(file("backdrop", "backdrop.jpg", "image/jpeg", "backdrop mkv"))
                .param("title", "Uploaded Matroska")
                .param("releaseYear", "2026")
                .param("genres", "Botanique, Admin")
                .param("synopsis", "Description de test MKV")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assetStatus").value("REGISTERED"));

        CatalogVideo video = videos.findAllWithTitleGraph().stream()
            .filter(item -> "Uploaded Matroska".equals(item.getTitle().getTitle()))
            .findFirst()
            .orElseThrow();
        assertThat(video.getAssetFilename()).matches("[a-f0-9]{64}\\.mkv");
        assertThat(video.getDurationSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejectsInvalidUploadedMediaExtension() throws Exception {
        Cookie session = login();

        mockMvc.perform(multipart("/api/admin/videos")
                .file(file("media", "invalid.bin", "application/octet-stream", "bad"))
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
        UploadedVideo first = upload(session, "Admin Series Base", "base media");
        UploadedVideo second = upload(session, "Admin Series Episode", "episode media");

        mockMvc.perform(put("/api/admin/videos/" + first.videoId())
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Admin Series Base Updated\",\"genres\":[\"Botanique\",\"Admin\"],\"durationSeconds\":120}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Admin Series Base Updated"))
            .andExpect(jsonPath("$.genres[1]").value("Admin"));

        mockMvc.perform(post("/api/admin/videos/" + second.videoId() + "/link")
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetTitleId\":" + first.titleId() + ",\"seasonNumber\":1,\"episodeNumber\":2,\"label\":\"S1:E2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titleId").value(first.titleId()))
            .andExpect(jsonPath("$.type").value("SERIES"))
            .andExpect(jsonPath("$.episodeNumber").value(2));

        mockMvc.perform(put("/api/admin/videos/" + second.videoId() + "/order")
                .with(csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonNumber\":1,\"episodeNumber\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.episodeNumber").value(3));

        mockMvc.perform(post("/api/admin/videos/" + second.videoId() + "/unlink")
                .with(csrf())
                .cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("MOVIE"))
            .andExpect(jsonPath("$.seasonNumber").value(0));
    }

    private UploadedVideo upload(Cookie session, String title, String mediaContent) throws Exception {
        mockMvc.perform(multipart("/api/admin/videos")
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
            .andExpect(jsonPath("$.assetStatus").value("REGISTERED"));
        CatalogVideo video = videos.findAllWithTitleGraph().stream()
            .filter(item -> title.equals(item.getTitle().getTitle()))
            .findFirst()
            .orElseThrow();
        return new UploadedVideo(video.getId(), video.getTitle().getId(), video.getAssetFilename());
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

    private record UploadedVideo(Long videoId, Long titleId, String assetFilename) {
    }
}

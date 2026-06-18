package dev.sey.streamfolio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.repository.CatalogTitleRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "streamfolio.media.storage=classpath")
class DraftMediaSecurityIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CatalogTitleRepository titles;

    private MockMvc mockMvc;
    private Long draftVideoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        draftVideoId = createDraftVideo();
    }

    @Test
    void regularUserCannotLoadDraftPlayback() throws Exception {
        mockMvc.perform(get("/api/videos/" + draftVideoId).with(user("regular").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotStreamDraftMedia() throws Exception {
        mockMvc.perform(get("/api/videos/" + draftVideoId + "/stream").with(user("regular").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotReadDraftHls() throws Exception {
        mockMvc.perform(get("/api/videos/" + draftVideoId + "/hls/master.m3u8").with(user("regular").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotReadDraftSubtitles() throws Exception {
        mockMvc.perform(get("/api/videos/" + draftVideoId + "/subtitles").with(user("regular").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotReadDraftThumbnails() throws Exception {
        mockMvc.perform(get("/api/videos/" + draftVideoId + "/thumbnails/manifest.json").with(user("regular").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotAccessAdminMediaApi() throws Exception {
        mockMvc.perform(get("/api/admin/videos").with(user("regular").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanLoadExplicitDraftPreview() throws Exception {
        mockMvc.perform(get("/api/admin/videos/" + draftVideoId + "/preview").with(user("administrator").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.videoId").value(draftVideoId))
            .andExpect(jsonPath("$.streamUrl").value("/api/admin/videos/" + draftVideoId + "/preview/stream"))
            .andExpect(jsonPath("$.subtitlesUrl").value("/api/admin/videos/" + draftVideoId + "/preview/subtitles"));
    }

    private Long createDraftVideo() {
        String suffix = UUID.randomUUID().toString();
        CatalogTitle title = new CatalogTitle(
            "draft-security-" + suffix,
            "Draft Security " + suffix,
            ContentType.MOVIE,
            2026,
            "Tous publics",
            1,
            "Brouillon de test.",
            "Titre de test non publié.",
            "/assets/posters-clean/aurora-drift.svg?v6",
            "/assets/posters-clean/aurora-drift.svg?v6",
            -1000
        );
        title.addGenre("Security");
        CatalogVideo video = new CatalogVideo(0, 0, "Film", "Draft", 60, "aurora-drift.mp4", "aurora-drift.vtt");
        video.updatePublicationStatus(CatalogVideo.STATUS_DRAFT);
        title.addVideo(video);
        CatalogTitle saved = titles.saveAndFlush(title);
        assertThat(saved.getVideos()).hasSize(1);
        assertThat(saved.getVideos().get(0).getId()).isNotNull();
        return saved.getVideos().get(0).getId();
    }
}

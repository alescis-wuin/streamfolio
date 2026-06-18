package dev.sey.streamfolio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "streamfolio.media.storage=classpath")
class SecurityIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void csrfEndpointProvidesTokenMetadata() throws Exception {
        mockMvc.perform(get("/api/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
            .andExpect(jsonPath("$.parameterName").value("_csrf"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("XSRF-TOKEN")));
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alexis@example.dev\",\"password\":\"demo1234\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Action refusée ou jeton CSRF invalide."));
    }

    @Test
    void loginSetsHttpOnlyCookieAndDoesNotExposeTokenInJson() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alexis@example.dev\",\"password\":\"demo1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("alexis@example.dev"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(cookie().exists(SESSION_COOKIE))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
            .andReturn();

        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void protectedVideoEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/videos/1"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/videos/1/stream"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/videos/1/subtitles"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/videos/1/hls/master.m3u8"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/videos/1/hls/segment_000.ts"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/videos/1/progress")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionSeconds\":1,\"durationSeconds\":12}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMutatingRequestsRequireCsrfToken() throws Exception {
        Cookie session = login();

        mockMvc.perform(put("/api/videos/1/progress")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionSeconds\":1,\"durationSeconds\":12}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/titles/1/watchlist").cookie(session))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/logout").cookie(session))
            .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanLoadPlaybackAndProtectedStream() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streamUrl").value("/api/videos/1/stream"))
            .andExpect(jsonPath("$.streamingMode").value("MP4_ONLY"));

        mockMvc.perform(get("/api/videos/1/stream")
                .cookie(session)
                .header(HttpHeaders.RANGE, "bytes=0-1023"))
            .andExpect(status().isPartialContent())
            .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"));
    }

    @Test
    void h2ConsoleIsNotPublicOutsideDevProfile() throws Exception {
        mockMvc.perform(get("/h2-console/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesServerSession() throws Exception {
        Cookie session = login();

        mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge(SESSION_COOKIE, 0));

        mockMvc.perform(get("/api/me").cookie(session))
            .andExpect(status().isUnauthorized());
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
}

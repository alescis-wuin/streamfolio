package dev.sey.streamfolio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSetsHttpOnlyCookieAndDoesNotExposeTokenInJson() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
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
        mockMvc.perform(put("/api/videos/1/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionSeconds\":1,\"durationSeconds\":12}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanLoadPlaybackAndProtectedStream() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/videos/1").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streamUrl").value("/api/videos/1/stream"));

        mockMvc.perform(get("/api/videos/1/stream")
                .cookie(session)
                .header(HttpHeaders.RANGE, "bytes=0-1023"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"));
    }

    @Test
    void logoutInvalidatesServerSession() throws Exception {
        Cookie session = login();

        mockMvc.perform(post("/api/auth/logout").cookie(session))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(get("/api/me").cookie(session))
            .andExpect(status().isUnauthorized());
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
}

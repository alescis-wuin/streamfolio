package dev.sey.streamfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sey.streamfolio.domain.UserRole;
import dev.sey.streamfolio.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class RegistrationIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserAccountRepository users;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void registeredUserGetsUserRoleOnlyAndCannotAccessAdminEndpoints() throws Exception {
        String identifier = "viewer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.displayName").value(identifier))
            .andExpect(jsonPath("$.user.roles[0]").value("USER"))
            .andExpect(jsonPath("$.user.roles[1]").doesNotExist())
            .andExpect(cookie().exists(SESSION_COOKIE))
            .andReturn();

        users.findByEmail(RegistrationService.internalEmail(identifier)).ifPresentOrElse(
            user -> {
                assertThat(user.getRoles()).containsExactly(UserRole.USER);
                assertThat(user.hasRole(UserRole.ADMIN)).isFalse();
            },
            () -> { throw new AssertionError("Registered user was not persisted."); }
        );

        Cookie session = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/admin/videos").cookie(session))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Accès réservé à un administrateur."));
    }

    @Test
    void registrationAcceptsEmailIdentifier() throws Exception {
        String identifier = "viewer" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.dev";

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value(identifier))
            .andExpect(jsonPath("$.user.roles[0]").value("USER"));
    }

    @Test
    void registrationRejectsStrangeUsernameCharacters() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"bad-user\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Nom d'utilisateur invalide. Utilise uniquement lettres, chiffres et underscores."));
    }

    @Test
    void registrationRejectsInvalidEmailIdentifier() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"bad@example\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Adresse e-mail invalide."));
    }

    @Test
    void registrationRequiresCsrfToken() throws Exception {
        String identifier = "viewer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"password123\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void duplicateIdentifierIsRejected() throws Exception {
        String identifier = "viewer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String payload = "{\"identifier\":\"" + identifier + "\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Identifiant déjà utilisé."));
    }
}

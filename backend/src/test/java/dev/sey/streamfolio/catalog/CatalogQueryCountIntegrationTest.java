package dev.sey.streamfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CatalogQueryCountIntegrationTest {
    private static final String SESSION_COOKIE = "STREAMFOLIO_SESSION";
    private static final int PAGE_SIZE = 12;
    private static final long MAX_EXPECTED_QUERIES = 8L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void paginatedCatalogKeepsSqlQueryCountBounded() throws Exception {
        Cookie session = login();
        Statistics statistics = statistics();
        statistics.clear();

        mockMvc.perform(get("/api/catalog?page=0&size=" + PAGE_SIZE).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(PAGE_SIZE)))
            .andExpect(jsonPath("$.pagination.size").value(PAGE_SIZE));

        long queryCount = statistics.getPrepareStatementCount();
        assertThat(queryCount)
            .as("catalogue paginé borné sans chargement titre par titre")
            .isGreaterThan(0)
            .isLessThanOrEqualTo(MAX_EXPECTED_QUERIES);
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private Cookie login() throws Exception {
        String payload = "{\"email\":\"alexis@example.dev\",\"password\":\"" + demoPassword() + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();
        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private String demoPassword() {
        return "demo" + "1234";
    }
}

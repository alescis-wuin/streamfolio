package dev.sey.streamfolio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class AdminRoleInterceptorTest {
    private final AdminRoleInterceptor interceptor = new AdminRoleInterceptor();

    @Test
    void allowsAdminRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new UserAccount("admin@example.dev", "Admin", "hash", Set.of(UserRole.USER, UserRole.ADMIN)));

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void rejectsUserWithoutAdminRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUser", new UserAccount("user@example.dev", "User", "hash", Set.of(UserRole.USER)));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("ADMIN");
    }

    @Test
    void rejectsAnonymousRequest() {
        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("ADMIN");
    }
}

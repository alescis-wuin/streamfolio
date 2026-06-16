package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object user = request.getAttribute("authUser");
        if (user instanceof UserAccount account && account.hasRole(UserRole.ADMIN)) {
            return true;
        }
        throw new AccessDeniedException("Role ADMIN requis.");
    }
}

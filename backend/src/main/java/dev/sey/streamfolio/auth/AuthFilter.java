package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final SessionCookieService cookies;

    public AuthFilter(AuthService authService, SessionCookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        cookies.resolveToken(request)
            .flatMap(authService::findByToken)
            .ifPresent(user -> authenticate(request, user));
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, UserAccount user) {
        request.setAttribute("authUser", user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user,
            null,
            authorities(user)
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<SimpleGrantedAuthority> authorities(UserAccount user) {
        Set<UserRole> roles = user.getRoles().isEmpty() ? Set.of(UserRole.USER) : user.getRoles();
        return roles.stream()
            .map(UserRole::authority)
            .map(SimpleGrantedAuthority::new)
            .toList();
    }
}

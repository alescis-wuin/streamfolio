package dev.sey.streamfolio.config;

import dev.sey.streamfolio.auth.AuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    private final boolean h2ConsoleEnabled;

    public SecurityConfig(@Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) {
        this.h2ConsoleEnabled = h2ConsoleEnabled;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthFilter authFilter) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        return http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfRequestHandler)
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers(
                    "/", "/index.html", "/styles.css", "/media-admin.css", "/ui-overrides.css", "/csrf.js", "/app.js",
                    "/sw.js", "/manifest.json", "/manifest.webmanifest", "/favicon.ico"
                ).permitAll();
                authorize.requestMatchers("/js/**", "/posters/**", "/icons/**", "/assets/**").permitAll();
                authorize.requestMatchers("/api/health", "/api/csrf", "/api/auth/login", "/api/auth/logout").permitAll();
                if (h2ConsoleEnabled) {
                    authorize.requestMatchers("/h2-console/**").permitAll();
                }
                authorize.requestMatchers("/api/admin/**").hasRole("ADMIN");
                authorize.anyRequest().authenticated();
            })
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> writeJson(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Connexion requise."))
                .accessDeniedHandler((request, response, exception) -> writeJson(response, HttpStatus.FORBIDDEN, "Forbidden", "Action refus\u00E9e ou jeton CSRF invalide."))
            )
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private static void writeJson(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status.value() + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\",\"details\":[]}");
    }
}

package dev.sey.streamfolio.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegistrationService registrations;
    private final SessionCookieService cookies;

    public RegistrationController(RegistrationService registrations, SessionCookieService cookies) {
        this.registrations = registrations;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        LoginResult result = registrations.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.sessionCookie(result.token()).toString());
        return new LoginResponse(result.user());
    }
}

package dev.sey.streamfolio.auth;

import dev.sey.streamfolio.domain.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final SessionCookieService cookies;

    public AuthController(AuthService authService, SessionCookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.sessionCookie(result.token()).toString());
        return new LoginResponse(result.user());
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.resolveToken(request).ifPresent(authService::logout);
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.expiredCookie().toString());
    }

    @GetMapping("/me")
    public UserDto me(@RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return UserDto.from(authService.requireUser(user));
    }
}

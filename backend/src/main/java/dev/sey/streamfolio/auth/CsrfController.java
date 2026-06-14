package dev.sey.streamfolio.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CsrfController {
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return CsrfResponse.from(csrfToken);
    }
}

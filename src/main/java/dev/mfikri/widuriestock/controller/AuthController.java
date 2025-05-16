package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.TokenResponse;
import dev.mfikri.widuriestock.model.user.UserLoginRequest;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<TokenResponse> login (@RequestBody UserLoginRequest request) {
        TokenResponse token = authService.login(request);
        return WebResponse.<TokenResponse>builder()
                .data(token)
                .build();
    }

    @DeleteMapping(path = "/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> logout (User user) {
        authService.logout(user);
        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }
}

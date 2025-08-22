package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.user.AuthRefreshTokenRequest;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    public WebResponse<AuthTokenResponse> login(@RequestBody AuthLoginRequest request) {
        log.info("Receiving request to login.");
        AuthTokenResponse response = authService.login(request);

        return WebResponse.<AuthTokenResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping(path = "/refresh-token")
    public WebResponse<AuthTokenResponse> getNewAccessToken(@RequestBody AuthRefreshTokenRequest authRefreshTokenRequest) {
        log.info("Receiving request to get new access token.");
        AuthTokenResponse newAccessToken = authService.getNewAccessToken(authRefreshTokenRequest.getRefreshToken());

        return WebResponse.<AuthTokenResponse>builder()
                .data(newAccessToken)
                .build();
    }
}

package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AuthRefreshTokenRequest;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController()
@RequestMapping("/api/auth")
public class AuthController {
//    private final AuthenticationProvider authenticationProvider;
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AuthTokenResponse> login(@RequestBody AuthLoginRequest request) {
        AuthTokenResponse response = authService.login(request);

        return WebResponse.<AuthTokenResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping(path = "/refresh-token")
    public WebResponse<AuthTokenResponse> getNewAccessToken(@RequestBody AuthRefreshTokenRequest authRefreshTokenRequest) {
        AuthTokenResponse newAccessToken = authService.getNewAccessToken(authRefreshTokenRequest.getRefreshToken());

        return WebResponse.<AuthTokenResponse>builder()
                .data(newAccessToken)
                .build();
    }

//    @DeleteMapping(path = "/logout",
//            produces = MediaType.APPLICATION_JSON_VALUE
//    )
//    public WebResponse<String> logout (User user) {
//        authService.logout(user);
//        return WebResponse.<String>builder()
//                .data("OK")
//                .build();
//    }
}

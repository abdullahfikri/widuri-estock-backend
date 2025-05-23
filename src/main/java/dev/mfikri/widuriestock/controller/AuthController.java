package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
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
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController( AuthenticationManager authenticationManager, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @PostMapping(path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AuthTokenResponse> login (@RequestBody AuthLoginRequest request) {
//        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());
//
//        Authentication authenticate = authenticationManager.authenticate(authenticationRequest);
        AuthTokenResponse response = authService.login(request);

        return WebResponse.<AuthTokenResponse>builder()
                .data(response)
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

package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.RefreshToken;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    
    private final ValidationService validationService;
    private final RefreshTokenService refreshTokenService;

    private final AuthenticationManager authenticationManager;


    private final JwtUtil jwtUtil;
    private final Integer jwtTtl;

    public AuthServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           ValidationService validationService, RefreshTokenService refreshTokenService,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil,
                           @Value("${application.security.jwt-ttl}") Integer jwtTtl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.validationService = validationService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.jwtTtl = jwtTtl;
    }

    @Override
    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        log.info("Processing request login. username={}", request.getUsername());

        validationService.validate(request);

        log.debug("Authenticating user via AuthenticationManager. username={}", request.getUsername() );
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(authenticationRequest);
        } catch (BadCredentialsException e) {
            log.warn("Bad credentials attempt. username={}", request.getUsername());
            throw  e;
        }

        String authenticatedUsername = authenticate.getName();

        log.debug("Generating new access token. username={}", authenticatedUsername);
        String token = jwtUtil.generate(authenticatedUsername, jwtTtl);

        log.info("Successfully logged in. username={}", authenticatedUsername);
        return AuthTokenResponse.builder()
                .accessToken(token)
                .refreshToken(refreshTokenService.create(authenticatedUsername, request.getUserAgent()).getRefreshToken())
                .build();
    }

    public AuthTokenResponse getNewAccessToken(String refreshTokenString) {
        log.info("Processing request to get a new Access token using refresh token.");

        if (refreshTokenString == null || refreshTokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token must not blank");
        }

        log.debug("Finding refresh token in the database.");
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token"));

        log.debug("Verifying if refresh token is expired. tokenId={}", refreshToken.getId());
        if (refreshTokenService.isTokenExpired(refreshToken)) {
            refreshTokenService.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token is expired");
        }
        String username = refreshToken.getUser().getUsername();
        log.debug("Generating new access token for user. username={}", username);
        String accessToken = jwtUtil.generate(username, jwtTtl);

        log.info("Successfully generated new access token for user. username={}", username);
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .build();
    }

//    @Override
//    @Transactional
//    public void logout(User user) {
//        user.setToken(null);
//        user.setTokenExpiredAt(null);
//
//        userRepository.save(user);
//    }

//    private Long next30Days() {
//        return System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
//    }
}

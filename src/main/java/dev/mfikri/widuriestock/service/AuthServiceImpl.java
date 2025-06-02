package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.RefreshToken;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
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
        validationService.validate(request);

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authenticationRequest);

        String token = jwtUtil.generate(authenticate.getName(), jwtTtl);

        return AuthTokenResponse.builder()
                .accessToken(token)
                .refreshToken(refreshTokenService.create(request.getUsername(), request.getUserAgent()).getRefreshToken())
                .build();
    }

    public AuthTokenResponse getNewAccessToken(String refreshTokenString) {
        if (refreshTokenString == null || refreshTokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token"));

        if (refreshTokenService.isTokenExpired(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is expired");
        }

        String accessToken = jwtUtil.generate(refreshToken.getUser().getUsername(), jwtTtl);

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

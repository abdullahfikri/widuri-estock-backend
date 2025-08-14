package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.RefreshToken;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token.expiration-ms}")
    private Long refreshTokenDuration;

    public RefreshTokenServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken create(String username, String userAgent) {
        log.info("Processing create refresh token request, username={}, userAgent={}", username, userAgent);
        
        User user= findUserByUsernameOrThrows(username);

        RefreshToken refreshToken = buildRefreshToken(user, userAgent);

        RefreshToken token = refreshTokenRepository.save(refreshToken);

        log.info("Created refresh token, username={}, userAgent={}", username, userAgent);
        return token;
    }

    private RefreshToken buildRefreshToken(User user, String userAgent) {
        return RefreshToken.builder()
                .user(user)
                .refreshToken(UUID.randomUUID().toString())
                .expiredAt(Instant.now().plusMillis(refreshTokenDuration))
                .userAgent(userAgent)
                .build();
    }

    private User findUserByUsernameOrThrows(String username) {
        return userRepository.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with username " + username + " not found."));
    }

    @Override
    public Optional<RefreshToken> findByToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken);
    }

    @Override
    public boolean isTokenExpired(RefreshToken refreshToken) {
        return refreshToken.getExpiredAt().isBefore(Instant.now());
    }

    @Override
    public void delete(RefreshToken refreshToken) {
        log.info("Deleting refresh token for user: {}", refreshToken.getUser().getUsername());
        refreshTokenRepository.delete(refreshToken);
    }
}

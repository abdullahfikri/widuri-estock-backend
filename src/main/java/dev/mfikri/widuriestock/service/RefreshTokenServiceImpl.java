package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.RefreshToken;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken create(String username, String userAgent) {
        User user=userRepository.findById(username).orElse(null);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .refreshToken(UUID.randomUUID().toString())
                .expiredAt(Instant.now().plusMillis(600000)) // 10 minute for development
                .userAgent(userAgent)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken);
    }

    @Override
    public boolean isTokenExpired(RefreshToken refreshToken) {
        if (refreshToken.getExpiredAt().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            return true;
        }
        return false;
    }
}

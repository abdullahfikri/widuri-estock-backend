package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken create(String username, String userAgent);
    Optional<RefreshToken> findByToken(String refreshToken);
    boolean isTokenExpired(RefreshToken refreshToken);
    void delete(RefreshToken refreshToken);
}

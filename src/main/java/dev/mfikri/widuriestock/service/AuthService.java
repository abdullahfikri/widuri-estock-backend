package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;

public interface AuthService {
    AuthTokenResponse login(AuthLoginRequest request);
    void logout(User user);
}

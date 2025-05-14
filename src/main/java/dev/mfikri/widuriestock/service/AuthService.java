package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.TokenResponse;
import dev.mfikri.widuriestock.model.user.UserLoginRequest;

public interface AuthService {
    TokenResponse login(UserLoginRequest request);
    void logout(User user);
}

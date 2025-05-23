package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final AuthenticationManager authenticationManager;


    public AuthServiceImpl(UserRepository userRepository, ValidationService validationService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        validationService.validate(request);

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());
        log.info(String.valueOf(Objects.isNull(authenticationRequest.getCredentials())));

        Authentication authenticate = authenticationManager.authenticate(authenticationRequest);

        log.info(String.valueOf(Objects.isNull(authenticate.getCredentials())));

        return AuthTokenResponse.builder()
                .token(UUID.randomUUID().toString())
                .expiredAt(next30Days())
                .build();

//        User user = userRepository.findById(request.getUsername())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));
//
//        if (BCrypt.checkpw(request.getPassword(), user.getPassword())) {
//            user.setToken(UUID.randomUUID().toString());
//            user.setTokenExpiredAt(next30Days());
//            userRepository.save(user);
//
//            return TokenResponse.builder()
//                    .token(user.getToken())
//                    .expiredAt(user.getTokenExpiredAt())
//                    .build();
//        } else {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong");
//        }

    }

    @Override
    @Transactional
    public void logout(User user) {
        user.setToken(null);
        user.setTokenExpiredAt(null);

        userRepository.save(user);
    }

    private Long next30Days() {
        return System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
    }
}

package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.RefreshToken;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AuthRefreshTokenRequest;
import dev.mfikri.widuriestock.model.user.AuthTokenResponse;
import dev.mfikri.widuriestock.model.user.AuthLoginRequest;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("owner");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("owner_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole("OWNER");

        userRepository.save(user);
    }

    @Test
    void loginFailedValidation() throws Exception {
        AuthLoginRequest request =  new AuthLoginRequest();
        request.setUsername("");
        request.setPassword("");
        request.setUserAgent("");

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
        });
    }

    @Test
    void loginFailedWrongIdOrPassword() throws Exception {
        AuthLoginRequest request =  new AuthLoginRequest();

        // username wrong
        request.setUsername("wronguser");
        request.setPassword("owner_password");
        request.setUserAgent("Android - Mozilla");

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Username or password wrong", response.getErrors());
        });

        // password wrong
        request.setUsername("owner");
        request.setPassword("wrongpassword");

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Username or password wrong", response.getErrors());
        });
    }

    @Test
    void loginSuccess() throws Exception {
        AuthLoginRequest request =  new AuthLoginRequest();
        request.setUsername("owner");
        request.setPassword("owner_password");
        request.setUserAgent("Android - Mozilla");


        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getAccessToken());
            assertNotNull(response.getData().getRefreshToken());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(response.getData().getRefreshToken()).orElse(null);
            assertNotNull(refreshToken);
            assertNotNull(refreshToken.getUser());
            assertEquals(response.getData().getRefreshToken(), refreshToken.getRefreshToken());
            assertEquals("owner", refreshToken.getUser().getUsername());

            log.info(response.getData().getAccessToken());
            log.info(response.getData().getRefreshToken());
        });
    }

    @Test
    void requestNewAccessTokenFailedRefreshTokenInvalid() throws Exception{
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        RefreshToken refreshTokenCreate = RefreshToken.builder()
                .user(user)
                .refreshToken("TOKENEXAMPLE")
                .expiredAt(Instant.now().minusMillis(10000))
                .userAgent("Android - Mozilla")
                .build();

        refreshTokenRepository.save(refreshTokenCreate);

        AuthRefreshTokenRequest request = new AuthRefreshTokenRequest();
        request.setRefreshToken("WRONGTOKEN");


        mockMvc.perform(
                post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid Token", response.getErrors());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).orElse(null);
            assertNull(refreshToken);

        });

        AuthRefreshTokenRequest requestNull = new AuthRefreshTokenRequest();
        requestNull.setRefreshToken(null);

        mockMvc.perform(
                post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestNull))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid Token", response.getErrors());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).orElse(null);
            assertNull(refreshToken);

        });

        AuthRefreshTokenRequest requestBlank = new AuthRefreshTokenRequest();
        requestBlank.setRefreshToken(null);

        mockMvc.perform(
                post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBlank))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid Token", response.getErrors());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).orElse(null);
            assertNull(refreshToken);

        });
    }

    @Test
    void requestNewAccessTokenFailedRefreshTokenExpired() throws Exception{
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        RefreshToken refreshTokenCreate = RefreshToken.builder()
                .user(user)
                .refreshToken("TOKENEXAMPLE")
                .expiredAt(Instant.now().minusMillis(10000))
                .userAgent("Android - Mozilla")
                .build();

        refreshTokenRepository.save(refreshTokenCreate);

        AuthRefreshTokenRequest request = new AuthRefreshTokenRequest();
        request.setRefreshToken(refreshTokenCreate.getRefreshToken());


        mockMvc.perform(
                post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Token is expired", response.getErrors());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).orElse(null);
            assertNull(refreshToken);

        });
    }

    @Test
    void requestNewAccessTokenSuccess() throws Exception{
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        RefreshToken refreshTokenCreate = RefreshToken.builder()
                .user(user)
                .refreshToken("TOKENEXAMPLE")
                .expiredAt(Instant.now().plusMillis(10000))
                .userAgent("Android - Mozilla")
                .build();

        refreshTokenRepository.save(refreshTokenCreate);

        AuthRefreshTokenRequest request = new AuthRefreshTokenRequest();
        request.setRefreshToken(refreshTokenCreate.getRefreshToken());


        mockMvc.perform(
                post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AuthTokenResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getAccessToken());
            assertNotNull(response.getData().getRefreshToken());

            RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(response.getData().getRefreshToken()).orElse(null);
            assertNotNull(refreshToken);
            assertNotNull(refreshToken.getUser());
            assertEquals(response.getData().getRefreshToken(), refreshToken.getRefreshToken());
            assertEquals("owner", refreshToken.getUser().getUsername());

            log.info(response.getData().getAccessToken());
            log.info(response.getData().getRefreshToken());
        });
    }

    //    @Test
//    void logoutFailedByNotPassingToken() throws Exception {
//        mockMvc.perform(
//                delete("/api/auth/logout")
//                        .accept(MediaType.APPLICATION_JSON)
//        ).andExpectAll(
//                status().isUnauthorized()
//        ).andDo(result -> {
//            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
//            });
//
//            assertNull(response.getData());
//            assertNotNull(response.getErrors());
//            assertEquals("Unauthenticated request", response.getErrors());
//        });
//    }
//
//    @Test
//    void logoutFailedByWrongToken() throws Exception {
//        User user = userRepository.findById("admin").orElse(null);
//        assertNotNull(user);
//        user.setToken("TOKENTEST");
//        user.setTokenExpiredAt(System.currentTimeMillis() + (1000L * 60));
//        userRepository.save(user);
//
//        mockMvc.perform(
//                delete("/api/auth/logout")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("X-API-TOKEN", "WRONGTOKEN")
//        ).andExpectAll(
//                status().isUnauthorized()
//        ).andDo(result -> {
//            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
//            });
//
//            assertNull(response.getData());
//            assertNotNull(response.getErrors());
//            assertEquals("Unauthenticated request", response.getErrors());
//        });
//    }
//
//    @Test
//    void logoutFailedByExpiredToken() throws Exception {
//        User user = userRepository.findById("admin").orElse(null);
//        assertNotNull(user);
//        user.setToken("TOKENTEST");
//        // set expired time to minus 1 minute to current time
//        user.setTokenExpiredAt(System.currentTimeMillis() - (1000L * 60));
//        userRepository.save(user);
//
//        mockMvc.perform(
//                delete("/api/auth/logout")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("X-API-TOKEN", "TOKENTEST")
//        ).andExpectAll(
//                status().isUnauthorized()
//        ).andDo(result -> {
//            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
//            });
//
//            assertNull(response.getData());
//            assertNotNull(response.getErrors());
//            assertEquals("Unauthenticated request", response.getErrors());
//        });
//    }
//
//    @Test
//    void logoutSuccess() throws Exception {
//        User user = userRepository.findById("admin").orElse(null);
//        assertNotNull(user);
//        user.setToken("TOKENTEST");
//        user.setTokenExpiredAt(System.currentTimeMillis() + (1000L * 60));
//        userRepository.save(user);
//
//        mockMvc.perform(
//                delete("/api/auth/logout")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("X-API-TOKEN", "TOKENTEST")
//        ).andExpectAll(
//                status().isOk()
//        ).andDo(result -> {
//            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
//            });
//
//            assertNull(response.getErrors());
//            assertNotNull(response.getData());
//            assertEquals("OK", response.getData());
//        });
//    }
}
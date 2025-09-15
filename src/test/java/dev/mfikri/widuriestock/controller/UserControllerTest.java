package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.user.UserResponse;
import dev.mfikri.widuriestock.model.user.UserSearchResponse;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("owner");
        user.setPassword(passwordEncoder.encode("owner123"));
        user.setFirstName("owner");
        user.setPhone("+000000000");
        user.setRole("OWNER");
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);
    }

    @Test
    void createFailedValidation() throws Exception{
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "");
        params.add("password", "");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", Role.ADMIN_WAREHOUSE.toString());

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");




        mockMvc.perform(
               multipart("/api/users")
                       .file(new MockMultipartFile("photo", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
                       .header("Authorization", authorizationToken)
                       .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                       .params(params)
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedInvalidRole() throws Exception{
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "adminwarehouse");
        params.add("password", "adminwarehouse");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", "WRONG ROLE");

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");

        mockMvc.perform(
                multipart("/api/users")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedInvalidPhotoFormat() throws Exception{
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "adminwarehouse");
        params.add("password", "adminwarehouse");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", Role.ADMIN_WAREHOUSE.toString());

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");

        mockMvc.perform(
                multipart("/api/users")
                        .file(new MockMultipartFile("photo", "wrong-image.sql", "sql", getClass().getResourceAsStream("/images/wrong-image.sql")))
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
            assertEquals("Image photo is not valid", response.getErrors());
        });
    }

    @Test
    void createFailedUsernameExists() throws Exception{
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword("adminwarehouse");
        user.setFirstName("adminwarehouse");
        user.setPhone("6200312300");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "adminwarehouse");
        params.add("password", "adminwarehouse");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", Role.ADMIN_WAREHOUSE.toString());

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");




        mockMvc.perform(
                multipart("/api/users")
                        .file(new MockMultipartFile("photo", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
            status().isConflict()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Username is already registered. Please use a different username.", response.getErrors());
        });
    }

    @Test
    void createFailedEmailExists() throws Exception{
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword("adminwarehouse123");
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "adminwarehouse123");
        params.add("password", "adminwarehouse123");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", Role.ADMIN_WAREHOUSE.toString());

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");

        mockMvc.perform(
                multipart("/api/users")
                        .file(new MockMultipartFile("photo", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
                status().isConflict()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Email is already registered. Please use a different email.", response.getErrors());
        });
    }

    @Test
    void createSuccess() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "adminwarehouse123");
        params.add("password", "adminwarehouse123");
        params.add("firstName", "John");
        params.add("lastName", "Doe");
        params.add("phone", "623123123");
        params.add("email", "john@doe.com");
        params.add("role", Role.ADMIN_WAREHOUSE.toString());

        params.add("address.street", "JLN Pancoran");
        params.add("address.village", "Kel. Bancakan");
        params.add("address.district", "Kec. Bocoran");
        params.add("address.city", "Meikarta City");
        params.add("address.province", "Jerez");
        params.add("address.country", "Katuliswi");
        params.add("address.postalCode", "00000012");




        mockMvc.perform(
                multipart(HttpMethod.POST, "/api/users")
                        .file(new MockMultipartFile("photo", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
            status().isCreated()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals(params.getFirst("username"), response.getData().getUsername());
            assertEquals(params.getFirst("firstName"), response.getData().getFirstName());
            assertEquals(params.getFirst("lastName"), response.getData().getLastName());
            assertEquals(params.getFirst("phone"), response.getData().getPhone());
            assertEquals(params.getFirst("email"), response.getData().getEmail());
            assertEquals(params.getFirst("role"), response.getData().getRole());
            AddressResponse addressResponse = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressResponse);
            assertNotNull(addressResponse.getId());
            assertEquals(params.getFirst("address.street"), addressResponse.getStreet());
            assertEquals(params.getFirst("address.village"), addressResponse.getVillage());
            assertEquals(params.getFirst("address.district"), addressResponse.getDistrict());
            assertEquals(params.getFirst("address.city"), addressResponse.getCity());
            assertEquals(params.getFirst("address.province"), addressResponse.getProvince());
            assertEquals(params.getFirst("address.country"), addressResponse.getCountry());
            assertEquals(params.getFirst("address.postalCode"), addressResponse.getPostalCode());

            assertTrue(userRepository.existsById(response.getData().getUsername()));
            assertTrue(userRepository.existsByAddressesId(addressResponse.getId()));
        });
    }

    @Test
    void getUserFailedUserNotFound() throws Exception{
        mockMvc.perform(
                get("/api/users/not-found")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());

            assertEquals("User is not found.", response.getErrors());
        });

        mockMvc.perform(
                get("/api/users/  ")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());

            assertEquals("Username must not blank.", response.getErrors());
        });
    }

    @Test
    void getUserSuccess() throws Exception{
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword("adminwarehouse123");
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);
        Address address = new Address();
        address.setStreet("Street 123");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000321");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                get("/api/users/ adminwarehouse")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(user.getFirstName(), response.getData().getFirstName());
            assertEquals(user.getLastName(), response.getData().getLastName());
            assertEquals(user.getPhone(), response.getData().getPhone());
            assertEquals(user.getEmail(), response.getData().getEmail());
            assertEquals(user.getRole(), response.getData().getRole());
            AddressResponse addressResponse = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressResponse);
            assertNotNull(addressResponse.getId());

            assertEquals(address.getStreet(), addressResponse.getStreet());
            assertEquals(address.getVillage(), addressResponse.getVillage());
            assertEquals(address.getDistrict(), addressResponse.getDistrict());
            assertEquals(address.getCity(), addressResponse.getCity());
            assertEquals(address.getProvince(), addressResponse.getProvince());
            assertEquals(address.getCountry(), addressResponse.getCountry());
            assertEquals(address.getPostalCode(), addressResponse.getPostalCode());
            assertEquals(user.getUsername(), addressResponse.getUsernameId());

            assertTrue(userRepository.existsById(response.getData().getUsername()));
            assertTrue(userRepository.existsByAddressesId(addressResponse.getId()));
        });
    }

    @Test
    void updateFailedValidation() throws Exception {
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword("adminwarehouse123");
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);
        Address address = new Address();
        address.setStreet("Street 123");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000321");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                patch("/api/users/adminwarehouse")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("password", "123a")
                        .param("email", "johndo123.com")
                        .param("role", "Wrong ROle")
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void updateFailedUsernameNotfound() throws Exception {

        mockMvc.perform(
                patch("/api/users/adminwarehouse")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("password", "abcd1234")
                        .param("email", "johndo123@example.com")
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("User is not found.", response.getErrors());
        });
    }

    @Test
    void updateSuccess() throws Exception {
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword(passwordEncoder.encode("adminwarehouse123"));
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);
        Address address = new Address();
        address.setStreet("Street 123");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000321");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                multipart(HttpMethod.PATCH, "/api/users/adminwarehouse")
                        .file(new MockMultipartFile("photo", "1.jpg", "image/jpg", getClass().getResourceAsStream("/images/1.jpg")))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("password", "abcd1234")
                        .param("firstName", "John Update")
                        .param("lastName", "Doe Update")
                        .param("phone", "623123123")
                        .param("email", "johndo123@example.com")
                        .param("role", Role.ADMIN_SELLER.toString())

        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals("John Update", response.getData().getFirstName());
            assertEquals("Doe Update", response.getData().getLastName());
            assertEquals("623123123", response.getData().getPhone());
            assertEquals("johndo123@example.com", response.getData().getEmail());
            assertEquals(Role.ADMIN_SELLER.toString(), response.getData().getRole());
            assertEquals(1, response.getData().getAddresses().size());

            User userUpdated = userRepository.findById(user.getUsername()).orElse(null);
            assertNotNull(userUpdated);
//            assertTrue(BCrypt.checkpw("abcd1234", userUpdated.getPassword().replace("{bcrypt}", "")));
            assertTrue(passwordEncoder.matches("abcd1234", userUpdated.getPassword()));
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals("John Update", userUpdated.getFirstName());
            assertEquals("Doe Update", userUpdated.getLastName());
            assertEquals("623123123", userUpdated.getPhone());
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals(Role.ADMIN_SELLER.toString(), userUpdated.getRole());
        });
    }

    @Test
    void getUserCurrentFailedTokenNotSend() throws Exception{
        mockMvc.perform(
                get("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());

            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getUserCurrentSuccess() throws Exception{
        mockMvc.perform(
                get("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("owner", response.getData().getUsername());
            assertEquals("owner", response.getData().getFirstName());
            assertNull(response.getData().getLastName());
            assertEquals("+000000000", response.getData().getPhone());
            assertEquals("OWNER", response.getData().getRole());

            assertTrue(userRepository.existsById(response.getData().getUsername()));
        });
    }


    @Test
    @Transactional
    void updateUserCurrentFailedValidation() throws Exception {
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword("adminwarehouse123");
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());

        userRepository.save(user);
        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        Address address = new Address();
        address.setStreet("Street 123");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000321");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                patch("/api/users/current")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("password", "123a")
                        .param("email", "johndo123.com")
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void updateUserCurrentFailedTokenNotSend() throws Exception {

        mockMvc.perform(
                patch("/api/users/current")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("password", "abcd1234")
                        .param("email", "johndo123@example.com")
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void updateUserCurrentSuccess() throws Exception {
        User user = new User();
        user.setUsername("adminwarehouse");
        user.setPassword(passwordEncoder.encode("admin_warehouse"));
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
        user.setEmail("john@doe.com");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);
        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        log.info(authorizationToken);

        Address address = new Address();
        address.setStreet("Street 123");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000321");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                multipart(HttpMethod.PATCH, "/api/users/current")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("password", "abcd1234")
                        .param("firstName", "John Update")
                        .param("lastName", "Doe Update")
                        .param("phone", "623123123")
                        .param("email", "johndo123@example.com")
                        .param("role", Role.ADMIN_SELLER.toString())

        ).andExpectAll(
                status().isOk()
         ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals("John Update", response.getData().getFirstName());
            assertEquals("Doe Update", response.getData().getLastName());
            assertEquals("623123123", response.getData().getPhone());
            assertEquals("johndo123@example.com", response.getData().getEmail());
            assertEquals(Role.ADMIN_WAREHOUSE.toString(), response.getData().getRole());
            assertEquals(1, response.getData().getAddresses().size());

            User userUpdated = userRepository.findById(user.getUsername()).orElse(null);
            assertNotNull(userUpdated);
            assertTrue(passwordEncoder.matches("abcd1234", userUpdated.getPassword()));
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals("John Update", userUpdated.getFirstName());
            assertEquals("Doe Update", userUpdated.getLastName());
            assertEquals("623123123", userUpdated.getPhone());
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals(Role.ADMIN_WAREHOUSE.toString(), userUpdated.getRole());
        });
    }

    @Test
    void searchAll() throws Exception {
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300");
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(11, response.getPaging().getTotalPage()); // 100 adminwarehouse + 1 owner = 101
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }

    @Test
    void searchByUsername() throws Exception {
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300" + i);
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("username", "warehouse1") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }

    @Test
    void searchByName() throws Exception{
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300" + i);
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("name", "warehouse 1") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }

    @Test
    void searchByEmailAndPhone() throws Exception{
        // email
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300" + i);
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("email", "john1") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });

        // phone

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("phone", "62003123001") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });

        // combine

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("email", "john1") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
                        .param("phone", "620031230010") // 10. Total = 1 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(1, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(1, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }

    @Test
    void searchByRole() throws Exception{
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300" + i);
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("role", Role.ADMIN_WAREHOUSE.toString()) //  Total = 100 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(10, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(10, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }

    @Test
    void searchCombine() throws Exception{

        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("adminwarehouse" + i);
            user.setPassword("adminwarehouse123");
            user.setFirstName("adminwarehouse " + i);
            user.setPhone("6200312300" + i);
            user.setEmail("john"+ i +"@doe.com");
            user.setRole(Role.ADMIN_WAREHOUSE.toString());
            userRepository.save(user);
        }

        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .param("role", Role.ADMIN_WAREHOUSE.toString()) //  Total = 100 Items
                        .param("email", "john1") // 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19. Total = 11 Items
                        .param("phone", "620031230010") // 10. Total = 1 Items
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<UserSearchResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(1, response.getData().size());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(1, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());
        });
    }


    // test authorization


    @Test
    void testAdminWarehouse() throws Exception{
        User user = new User();
        user.setUsername("adminwhs");
        user.setPassword(passwordEncoder.encode("adminwhs_password"));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        // create
        mockMvc.perform(
                post("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // search
        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // get
        mockMvc.perform(
                get("/api/users/adminwhs")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // update
        mockMvc.perform(
                patch("/api/users/adminwhs")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // get current
        mockMvc.perform(
                get("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(user.getFirstName(), response.getData().getFirstName());
            assertEquals(user.getPhone(), response.getData().getPhone());
            assertEquals(user.getEmail(), response.getData().getEmail());
            assertEquals(user.getLastName(), response.getData().getLastName());
            assertEquals(user.getRole(), response.getData().getRole());
        });

        // update current
        mockMvc.perform(
                patch("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .param("password", "abcd1234")
                        .param("firstName", "John Update")
                        .param("lastName", "Doe Update")
                        .param("phone", "623123123")
                        .param("email", "johndo123@example.com")
                        .param("role", Role.ADMIN_SELLER.toString())
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals("John Update", response.getData().getFirstName());
            assertEquals("Doe Update", response.getData().getLastName());
            assertEquals("623123123", response.getData().getPhone());
            assertEquals("johndo123@example.com", response.getData().getEmail());
            assertEquals(Role.ADMIN_WAREHOUSE.toString(), response.getData().getRole());
        });
    }

    @Test
    void testAdminSeller() throws Exception{
        User user = new User();
        user.setUsername("adminslr");
        user.setPassword(passwordEncoder.encode("adminslr_password"));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_SELLER.toString());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        // create
        mockMvc.perform(
                post("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // search
        mockMvc.perform(
                get("/api/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // get
        mockMvc.perform(
                get("/api/users/adminwhs")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // update
        mockMvc.perform(
                patch("/api/users/adminwhs")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        // get current
        mockMvc.perform(
                get("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(user.getFirstName(), response.getData().getFirstName());
            assertEquals(user.getPhone(), response.getData().getPhone());
            assertEquals(user.getEmail(), response.getData().getEmail());
            assertEquals(user.getLastName(), response.getData().getLastName());
            assertEquals(user.getRole(), response.getData().getRole());
        });

        // update current
        mockMvc.perform(
                patch("/api/users/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .param("password", "abcd1234")
                        .param("firstName", "John Update")
                        .param("lastName", "Doe Update")
                        .param("phone", "623123123")
                        .param("email", "johndo123@example.com")
                        .param("role", Role.ADMIN_WAREHOUSE.toString())
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<UserResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals("John Update", response.getData().getFirstName());
            assertEquals("Doe Update", response.getData().getLastName());
            assertEquals("623123123", response.getData().getPhone());
            assertEquals("johndo123@example.com", response.getData().getEmail());
            assertEquals(Role.ADMIN_SELLER.toString(), response.getData().getRole());
        });
    }
}
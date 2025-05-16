package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.AddressModel;
import dev.mfikri.widuriestock.model.user.UserResponse;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("admin");
        user.setPassword(BCrypt.hashpw("admin_warehouse", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole("OWNER");
        user.setToken("TOKENTEST");
        user.setTokenExpiredAt(System.currentTimeMillis() + (1000L * 60));
        userRepository.save(user);
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
                       .header("X-API-TOKEN", "TOKENTEST")
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
                        .file(new MockMultipartFile("photo", "wrong-image.sql", "sql", getClass().getResourceAsStream("/images/wrong-image.sql")))
                        .header("X-API-TOKEN", "TOKENTEST")
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
        user.setUsername("adminwarehouse123");
        user.setPassword("adminwarehouse123");
        user.setFirstName("adminwarehouse123");
        user.setPhone("6200312300");
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
                        .header("X-API-TOKEN", "TOKENTEST")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
            status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Username is already exists", response.getErrors());
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
                        .header("X-API-TOKEN", "TOKENTEST")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Email is already exists", response.getErrors());
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
            AddressModel addressModel = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressModel);
            assertNotNull(addressModel.getId());
            assertEquals(params.getFirst("address.street"), addressModel.getStreet());
            assertEquals(params.getFirst("address.village"), addressModel.getVillage());
            assertEquals(params.getFirst("address.district"), addressModel.getDistrict());
            assertEquals(params.getFirst("address.city"), addressModel.getCity());
            assertEquals(params.getFirst("address.province"), addressModel.getProvince());
            assertEquals(params.getFirst("address.country"), addressModel.getCountry());
            assertEquals(params.getFirst("address.postalCode"), addressModel.getPostalCode());

            assertTrue(userRepository.existsById(response.getData().getUsername()));
            assertTrue(userRepository.existsByAddressesId(addressModel.getId()));
        });
    }

    @Test
    void getUserFailedUserNotFound() throws Exception{
        mockMvc.perform(
                get("/api/users/not-found")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-API-TOKEN", "TOKENTEST")
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
            AddressModel addressModel = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressModel);
            assertNotNull(addressModel.getId());

            assertEquals(address.getStreet(), addressModel.getStreet());
            assertEquals(address.getVillage(), addressModel.getVillage());
            assertEquals(address.getDistrict(), addressModel.getDistrict());
            assertEquals(address.getCity(), addressModel.getCity());
            assertEquals(address.getProvince(), addressModel.getProvince());
            assertEquals(address.getCountry(), addressModel.getCountry());
            assertEquals(address.getPostalCode(), addressModel.getPostalCode());

            assertTrue(userRepository.existsById(response.getData().getUsername()));
            assertTrue(userRepository.existsByAddressesId(addressModel.getId()));
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
                        .header("X-API-TOKEN", "TOKENTEST")
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
                multipart(HttpMethod.PATCH, "/api/users/adminwarehouse")
                        .file(new MockMultipartFile("photo", "1.jpg", "image/jpg", getClass().getResourceAsStream("/images/1.jpg")))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-API-TOKEN", "TOKENTEST")
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

            assertTrue(BCrypt.checkpw("abcd1234", userUpdated.getPassword()));
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals("John Update", userUpdated.getFirstName());
            assertEquals("Doe Update", userUpdated.getLastName());
            assertEquals("623123123", userUpdated.getPhone());
            assertEquals("johndo123@example.com", userUpdated.getEmail());
            assertEquals(Role.ADMIN_SELLER.toString(), userUpdated.getRole());
        });
    }
}
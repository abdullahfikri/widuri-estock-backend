package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.address.AddressUpdateRequest;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.RefreshTokenRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class AddressControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";

    @BeforeEach
    @Transactional
    void setUp() {
        refreshTokenRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("owner");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("owner_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole("OWNER");
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);
    }

    @Test
    void createAddressFailedTokenNotSend() throws Exception {

        Address address = new Address();
        address.setStreet("Just Street");

        mockMvc.perform(
                post("/api/users/current/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createAddressFailedValidation() throws Exception {
        Address address = new Address();
        address.setStreet("Just Street");

        mockMvc.perform(
                post("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
        });
    }

    @Test
    void createAddressSuccess() throws Exception {
        AddressCreateRequest address = new AddressCreateRequest();
        address.setStreet("JLN Diponegoro");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");

        mockMvc.perform(
                post("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getCountry(), response.getData().getCountry());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals("owner", response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);

            assertNotNull(addressDb);
            assertEquals(address.getStreet(), addressDb.getStreet());
            assertEquals(address.getVillage(), addressDb.getVillage());
            assertEquals(address.getDistrict(), addressDb.getDistrict());
            assertEquals(address.getCity(), addressDb.getCity());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals(address.getCountry(), addressDb.getCountry());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals("owner", addressDb.getUser().getUsername());
        });
    }

    @Test
    void getListAddressFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                get("/api/users/current/addresses")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }


    @Test
    void getListAddressSuccess() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);
        for (int i = 0; i < 5; i++) {

            Address address = new Address();
            address.setUser(user);
            address.setStreet("JLN Diponegoro " + i);
            address.setVillage("Kel. Air Baru");
            address.setDistrict("Kec. Pantai Indah");
            address.setCity("Meikarta");
            address.setProvince("Jakarta");
            address.setCountry("Indonesia");
            address.setPostalCode("123123");

            addressRepository.save(address);

            Thread.sleep(1000);
        }


        mockMvc.perform(
                get("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<AddressResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(5, response.getData().size());

            AddressResponse firstAddress = response.getData().getFirst();
            assertNotNull(firstAddress);
            assertNotNull(firstAddress.getId());
            assertEquals("owner", firstAddress.getUsernameId());
            assertEquals("JLN Diponegoro 0", firstAddress.getStreet());
            assertEquals("Kel. Air Baru", firstAddress.getVillage());
            assertEquals("Kec. Pantai Indah", firstAddress.getDistrict());
            assertEquals("Meikarta", firstAddress.getCity());
            assertEquals("Jakarta", firstAddress.getProvince());
            assertEquals("Indonesia", firstAddress.getCountry());
            assertEquals("123123", firstAddress.getPostalCode());
        });
    }

    @Test
    void getAddressFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                get("/api/users/current/addresses/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getAddressFailedAddressNotFound() throws Exception {
        mockMvc.perform(
                get("/api/users/current/addresses/123")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());
        });
    }

    @Test
    void getAddressFailedUserAndAddressNotMatch() throws Exception {
        User user = new User();
        user.setUsername("TESTINGUSER");
        user.setPassword(BCrypt.hashpw("TESTINGPASSWORD", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        Address address = new Address();
        address.setUser(user);
        address.setStreet("JLN Diponegoro ");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");

        addressRepository.save(address);

        mockMvc.perform(
                get("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());
        });
    }

    @Test
    void getAddressSuccess() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        Address address = new Address();
        address.setStreet("Test street");
        address.setVillage("Test Village");
        address.setDistrict("Test District");
        address.setCity("Test City");
        address.setProvince("Test Province");
        address.setCountry("Test Country");
        address.setPostalCode("123123");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                get("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(address.getId(), response.getData().getId());
            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getPostalCode(), response.getData().getPostalCode());
        });
    }

    // update scenario
    @Test
    void updateAddressFailedTokenNotSend() throws Exception {

        Address address = new Address();
        address.setStreet("Just Street");

        mockMvc.perform(
                put("/api/users/current/addresses/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void updateAddressFailedValidation() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        Address address = new Address();
        address.setStreet("Test street");
        address.setVillage("Test Village");
        address.setDistrict("Test District");
        address.setCity("Test City");
        address.setProvince("Test Province");
        address.setCountry("Test Country");
        address.setPostalCode("123123");
        address.setUser(user);
        addressRepository.save(address);

        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro");
        updateRequest.setVillage("Kel. Air Baru");
        updateRequest.setDistrict("Kec. Pantai Indah");
        updateRequest.setCity("Meikarta");
        updateRequest.setProvince("Jakarta");


        mockMvc.perform(
                put("/api/users/current/addresses/" + address.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void updateAddressFailedAddressNotFound() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        Address address = new Address();
        address.setStreet("Test street");
        address.setVillage("Test Village");
        address.setDistrict("Test District");
        address.setCity("Test City");
        address.setProvince("Test Province");
        address.setCountry("Test Country");
        address.setPostalCode("123123");
        address.setUser(user);
        addressRepository.save(address);

        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro");
        updateRequest.setVillage("Kel. Air Baru");
        updateRequest.setDistrict("Kec. Pantai Indah");
        updateRequest.setCity("Meikarta");
        updateRequest.setProvince("Jakarta");
        updateRequest.setCountry("Indonesia");
        updateRequest.setPostalCode("123123");

        mockMvc.perform(
                put("/api/users/current/addresses/99999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());
        });
    }

    @Test
    void updateAddressFailedUserAndAddressNotMatch() throws Exception {
        User user = new User();
        user.setUsername("TESTINGUSER");
        user.setPassword(BCrypt.hashpw("TESTINGPASSWORD", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        Address address = new Address();
        address.setUser(user);
        address.setStreet("JLN Diponegoro ");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");
        addressRepository.save(address);

        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro");
        updateRequest.setVillage("Kel. Air Baru");
        updateRequest.setDistrict("Kec. Pantai Indah");
        updateRequest.setCity("Meikarta");
        updateRequest.setProvince("Jakarta");
        updateRequest.setCountry("Indonesia");
        updateRequest.setPostalCode("123123");


        mockMvc.perform(
                // not address belong to owner
                put("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());
        });
    }

    @Test
    void updateAddressSuccess() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        Address address = new Address();
        address.setStreet("Test street");
        address.setVillage("Test Village");
        address.setDistrict("Test District");
        address.setCity("Test City");
        address.setProvince("Test Province");
        address.setCountry("Test Country");
        address.setPostalCode("123123");
        address.setUser(user);
        addressRepository.save(address);


        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro");
        updateRequest.setVillage("Kel. Air Baru");
        updateRequest.setDistrict("Kec. Pantai Indah");
        updateRequest.setCity("Meikarta");
        updateRequest.setProvince("Jakarta");
        updateRequest.setCountry("Indonesia");
        updateRequest.setPostalCode("123123");

        mockMvc.perform(
                put("/api/users/current/addresses/" + address.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getId(), response.getData().getId());
            assertEquals(updateRequest.getStreet(), response.getData().getStreet());
            assertEquals(updateRequest.getVillage(), response.getData().getVillage());
            assertEquals(updateRequest.getDistrict(), response.getData().getDistrict());
            assertEquals(updateRequest.getCity(), response.getData().getCity());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals(updateRequest.getCountry(), response.getData().getCountry());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals("owner", response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);

            assertNotNull(addressDb);
            assertEquals(address.getId(), addressDb.getId());
            assertEquals(updateRequest.getStreet(), addressDb.getStreet());
            assertEquals(updateRequest.getVillage(), addressDb.getVillage());
            assertEquals(updateRequest.getDistrict(), addressDb.getDistrict());
            assertEquals(updateRequest.getCity(), addressDb.getCity());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals(updateRequest.getCountry(), addressDb.getCountry());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals("owner", addressDb.getUser().getUsername());
        });
    }

    // delete scenario

    @Test
    void deleteAddressFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                delete("/api/users/current/addresses/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void deleteAddressFailedAddressNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/users/current/addresses/123")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());

        });
    }

    @Test
    void deleteAddressFailedUserAndAddressNotMatch() throws Exception {
        User user = new User();
        user.setUsername("TESTINGUSER");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("TESTINGPASSWORD", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);

        Address address = new Address();
        address.setUser(user);
        address.setStreet("JLN Diponegoro ");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");

        addressRepository.save(address);

        mockMvc.perform(
                // not owner address
                delete("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Address is not found.", response.getErrors());
            Address deletedAddress = addressRepository.findById(address.getId()).orElse(null);
            assertNotNull(deletedAddress);
        });
    }

    @Test
    void deleteAddressSuccess() throws Exception {
        User user = userRepository.findById("owner").orElse(null);
        assertNotNull(user);

        Address address = new Address();
        address.setStreet("Test street");
        address.setVillage("Test Village");
        address.setDistrict("Test District");
        address.setCity("Test City");
        address.setProvince("Test Province");
        address.setCountry("Test Country");
        address.setPostalCode("123123");
        address.setUser(user);
        addressRepository.save(address);

        mockMvc.perform(
                delete("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            Address deletedAddress = addressRepository.findById(address.getId()).orElse(null);
            assertNull(deletedAddress);

        });
    }

    // test authorization


    @Test
    void testAdminWarehouse() throws Exception {
        User user = new User();
        user.setUsername("adminwhs");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("adminwhs_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.toString());
        userRepository.save(user);
        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        AddressCreateRequest address = new AddressCreateRequest();
        address.setStreet("JLN Diponegoro");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");

        // create
        mockMvc.perform(
                post("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getCountry(), response.getData().getCountry());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(user.getUsername(), response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(addressDb);

            assertEquals(address.getStreet(), addressDb.getStreet());
            assertEquals(address.getVillage(), addressDb.getVillage());
            assertEquals(address.getDistrict(), addressDb.getDistrict());
            assertEquals(address.getCity(), addressDb.getCity());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals(address.getCountry(), addressDb.getCountry());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals(user.getUsername(), addressDb.getUser().getUsername());

            address.setId(response.getData().getId());
        });

        // get list
        mockMvc.perform(
                get("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<AddressResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());

            AddressResponse firstAddress = response.getData().getFirst();
            assertNotNull(firstAddress);
            assertNotNull(firstAddress.getId());
            assertEquals(user.getUsername(), firstAddress.getUsernameId());
            assertEquals(address.getStreet(), firstAddress.getStreet());
            assertEquals(address.getVillage(), firstAddress.getVillage());
            assertEquals(address.getDistrict(), firstAddress.getDistrict());
            assertEquals(address.getCity(), firstAddress.getCity());
            assertEquals(address.getProvince(), firstAddress.getProvince());
            assertEquals(address.getCountry(), firstAddress.getCountry());
            assertEquals(address.getPostalCode(), firstAddress.getPostalCode());
        });


        // get single address
        mockMvc.perform(
                get("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(address.getId(), response.getData().getId());
            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getPostalCode(), response.getData().getPostalCode());
        });

        // update
        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro new");
        updateRequest.setVillage("Kel. Air Baru new");
        updateRequest.setDistrict("Kec. Pantai Indah new");
        updateRequest.setCity("Meikarta new");
        updateRequest.setProvince("Jakarta new");
        updateRequest.setCountry("Indonesia");
        updateRequest.setPostalCode("123123");

        mockMvc.perform(
                put("/api/users/current/addresses/" + address.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getId(), response.getData().getId());
            assertEquals(updateRequest.getStreet(), response.getData().getStreet());
            assertEquals(updateRequest.getVillage(), response.getData().getVillage());
            assertEquals(updateRequest.getDistrict(), response.getData().getDistrict());
            assertEquals(updateRequest.getCity(), response.getData().getCity());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals(updateRequest.getCountry(), response.getData().getCountry());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals(user.getUsername(), response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);

            assertNotNull(addressDb);
            assertEquals(address.getId(), addressDb.getId());
            assertEquals(updateRequest.getStreet(), addressDb.getStreet());
            assertEquals(updateRequest.getVillage(), addressDb.getVillage());
            assertEquals(updateRequest.getDistrict(), addressDb.getDistrict());
            assertEquals(updateRequest.getCity(), addressDb.getCity());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals(updateRequest.getCountry(), addressDb.getCountry());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals(user.getUsername(), addressDb.getUser().getUsername());
        });

        // delete
        mockMvc.perform(
                delete("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            Address deletedAddress = addressRepository.findById(address.getId()).orElse(null);
            assertNull(deletedAddress);

        });
    }

    @Test
    void testAdminSeller() throws Exception {
        User user = new User();
        user.setUsername("adminslr");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("adminslr_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_SELLER.toString());
        userRepository.save(user);
        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        AddressCreateRequest address = new AddressCreateRequest();
        address.setStreet("JLN Diponegoro");
        address.setVillage("Kel. Air Baru");
        address.setDistrict("Kec. Pantai Indah");
        address.setCity("Meikarta");
        address.setProvince("Jakarta");
        address.setCountry("Indonesia");
        address.setPostalCode("123123");

        // create
        mockMvc.perform(
                post("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getCountry(), response.getData().getCountry());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(user.getUsername(), response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(addressDb);

            assertEquals(address.getStreet(), addressDb.getStreet());
            assertEquals(address.getVillage(), addressDb.getVillage());
            assertEquals(address.getDistrict(), addressDb.getDistrict());
            assertEquals(address.getCity(), addressDb.getCity());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals(address.getCountry(), addressDb.getCountry());
            assertEquals(address.getProvince(), addressDb.getProvince());
            assertEquals(user.getUsername(), addressDb.getUser().getUsername());

            address.setId(response.getData().getId());
        });

        // get list
        mockMvc.perform(
                get("/api/users/current/addresses")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<AddressResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());

            AddressResponse firstAddress = response.getData().getFirst();
            assertNotNull(firstAddress);
            assertNotNull(firstAddress.getId());
            assertEquals(user.getUsername(), firstAddress.getUsernameId());
            assertEquals(address.getStreet(), firstAddress.getStreet());
            assertEquals(address.getVillage(), firstAddress.getVillage());
            assertEquals(address.getDistrict(), firstAddress.getDistrict());
            assertEquals(address.getCity(), firstAddress.getCity());
            assertEquals(address.getProvince(), firstAddress.getProvince());
            assertEquals(address.getCountry(), firstAddress.getCountry());
            assertEquals(address.getPostalCode(), firstAddress.getPostalCode());
        });


        // get single address
        mockMvc.perform(
                get("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(address.getId(), response.getData().getId());
            assertEquals(address.getStreet(), response.getData().getStreet());
            assertEquals(address.getVillage(), response.getData().getVillage());
            assertEquals(address.getDistrict(), response.getData().getDistrict());
            assertEquals(address.getCity(), response.getData().getCity());
            assertEquals(address.getProvince(), response.getData().getProvince());
            assertEquals(address.getPostalCode(), response.getData().getPostalCode());
        });

        // update
        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setStreet("JLN Diponegoro new");
        updateRequest.setVillage("Kel. Air Baru new");
        updateRequest.setDistrict("Kec. Pantai Indah new");
        updateRequest.setCity("Meikarta new");
        updateRequest.setProvince("Jakarta new");
        updateRequest.setCountry("Indonesia");
        updateRequest.setPostalCode("123123");

        mockMvc.perform(
                put("/api/users/current/addresses/" + address.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<AddressResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(address.getId(), response.getData().getId());
            assertEquals(updateRequest.getStreet(), response.getData().getStreet());
            assertEquals(updateRequest.getVillage(), response.getData().getVillage());
            assertEquals(updateRequest.getDistrict(), response.getData().getDistrict());
            assertEquals(updateRequest.getCity(), response.getData().getCity());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals(updateRequest.getCountry(), response.getData().getCountry());
            assertEquals(updateRequest.getProvince(), response.getData().getProvince());
            assertEquals(user.getUsername(), response.getData().getUsernameId());

            Address addressDb = addressRepository.findById(response.getData().getId()).orElse(null);

            assertNotNull(addressDb);
            assertEquals(address.getId(), addressDb.getId());
            assertEquals(updateRequest.getStreet(), addressDb.getStreet());
            assertEquals(updateRequest.getVillage(), addressDb.getVillage());
            assertEquals(updateRequest.getDistrict(), addressDb.getDistrict());
            assertEquals(updateRequest.getCity(), addressDb.getCity());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals(updateRequest.getCountry(), addressDb.getCountry());
            assertEquals(updateRequest.getProvince(), addressDb.getProvince());
            assertEquals(user.getUsername(), addressDb.getUser().getUsername());
        });

        // delete
        mockMvc.perform(
                delete("/api/users/current/addresses/" + address.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            Address deletedAddress = addressRepository.findById(address.getId()).orElse(null);
            assertNull(deletedAddress);

        });
    }
}
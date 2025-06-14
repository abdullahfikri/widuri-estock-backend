package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.SupplierRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class SupplierControllerTest {
    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;



    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();
        supplierRepository.deleteAll();

        User user = new User();
        user.setUsername("admin_warehouse");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("admin_warehouse_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.name());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);
    }

    @Test
    void createFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createFailedValidation() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("Supplier Test");

        mockMvc.perform(
                post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .content(objectMapper.writeValueAsString(supplier))
        ).andExpect(
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
    void createFailedDuplicateNameAndEmail() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("Supplier Test");
        supplier.setPhone("6281238218");
        supplier.setEmail("john@company.xyz");
        supplier.setInformation("Supplier for fishing tools");
        supplierRepository.save(supplier);

        Address address = new Address();
        address.setStreet("JL Test");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000000");
        address.setSupplier(supplier);
        addressRepository.save(address);

        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setSupplierName("Supplier Test");
        request.setPhone("6281238218");
        request.setEmail("johnupdated@company.xyz");
        request.setInformation("Supplier for fishing tools");
        request.setAddress(AddressCreateRequest.builder()
                        .street("JL Test")
                        .village("Village Test")
                        .district("District Test")
                        .city("City Test")
                        .province("Province Test")
                        .country("Country Test")
                        .postalCode("0000000")
                .build());


        mockMvc.perform(
                post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Supplier name is already exists.", response.getErrors());
        });


        // check duplicate email
        request.setSupplierName("Update SupplierName");
        request.setEmail("john@company.xyz");
        mockMvc.perform(
                post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Email is already exists.", response.getErrors());
        });
    }

    @Test
    void createSuccess() throws Exception {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setSupplierName("Supplier Test");
        request.setPhone("6281238218");
        request.setEmail("johnupdated@company.xyz");
        request.setInformation("Supplier for fishing tools");
        request.setAddress(AddressCreateRequest.builder()
                .street("JL Test")
                .village("Village Test")
                .district("District Test")
                .city("City Test")
                .province("Province Test")
                .country("Country Test")
                .postalCode("0000000")
                .build());


        mockMvc.perform(
                post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<SupplierResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getSupplierName(), response.getData().getSupplierName());
            assertEquals(request.getPhone(), response.getData().getPhone());
            assertEquals(request.getEmail(), response.getData().getEmail());
            assertEquals(request.getInformation(), response.getData().getInformation());
            AddressResponse addressResponse = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressResponse);
            assertNotNull(addressResponse.getId());
            assertEquals(request.getAddress().getStreet(), addressResponse.getStreet());
            assertEquals(request.getAddress().getVillage(), addressResponse.getVillage());
            assertEquals(request.getAddress().getDistrict(), addressResponse.getDistrict());
            assertEquals(request.getAddress().getCity(), addressResponse.getCity());
            assertEquals(request.getAddress().getProvince(), addressResponse.getProvince());
            assertEquals(request.getAddress().getCountry(), addressResponse.getCountry());
            assertEquals(request.getAddress().getPostalCode(), addressResponse.getPostalCode());

            // assert supplier is inserted to repository
            Supplier supplier = supplierRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(supplier);
            assertEquals(supplier.getId(), response.getData().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplierName());
            assertEquals(supplier.getPhone(), response.getData().getPhone());
            assertEquals(supplier.getEmail(), response.getData().getEmail());
            assertEquals(supplier.getInformation(), response.getData().getInformation());


            Address supplierAddress = addressRepository.findBySupplier(supplier);
            assertNotNull(supplierAddress);
            assertEquals(supplierAddress.getId(), addressResponse.getId());
            assertEquals(supplierAddress.getStreet(), addressResponse.getStreet());
            assertEquals(supplierAddress.getVillage(), addressResponse.getVillage());
            assertEquals(supplierAddress.getDistrict(), addressResponse.getDistrict());
            assertEquals(supplierAddress.getCity(), addressResponse.getCity());
            assertEquals(supplierAddress.getProvince(), addressResponse.getProvince());
            assertEquals(supplierAddress.getCountry(), addressResponse.getCountry());
            assertEquals(supplierAddress.getPostalCode(), addressResponse.getPostalCode());
        });
    }

    @Test
    void getFailedTokenNotSend() throws Exception {

        mockMvc.perform(
                get("/api/suppliers/11")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getFailedIdNotNumber() throws Exception {

        mockMvc.perform(
                get("/api/suppliers/abc")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Argument path type is wrong.", response.getErrors());
        });
    }

    @Test
    void getFailedSupplierNotFound() throws Exception {

        mockMvc.perform(
                get("/api/suppliers/123")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Supplier is not found.", response.getErrors());
        });
    }

    @Test
    void getSuccess() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("Supplier Test");
        supplier.setPhone("6281238218");
        supplier.setEmail("john@company.xyz");
        supplier.setInformation("Supplier for fishing tools");
        supplierRepository.save(supplier);

        Address address = new Address();
        address.setStreet("JL Test");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000000");
        address.setSupplier(supplier);
        addressRepository.save(address);

        mockMvc.perform(
                get("/api/suppliers/" + supplier.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<SupplierResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(supplier.getId(), response.getData().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplierName());
            assertEquals(supplier.getPhone(), response.getData().getPhone());
            assertEquals(supplier.getEmail(), response.getData().getEmail());
            assertEquals(supplier.getInformation(), response.getData().getInformation());
            AddressResponse addressResponse = response.getData().getAddresses().stream().findFirst().orElse(null);
            assertNotNull(addressResponse);
            assertEquals(address.getId(), addressResponse.getId());
            assertEquals(address.getStreet(), addressResponse.getStreet());
            assertEquals(address.getVillage(), addressResponse.getVillage());
            assertEquals(address.getDistrict(), addressResponse.getDistrict());
            assertEquals(address.getCity(), addressResponse.getCity());
            assertEquals(address.getProvince(), addressResponse.getProvince());
            assertEquals(address.getCountry(), addressResponse.getCountry());
            assertEquals(address.getPostalCode(), addressResponse.getPostalCode());
        });
    }

    @Test
    void deleteFailedTokenNotSend() throws Exception {

        mockMvc.perform(
                delete("/api/suppliers/11")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void deleteFailedIdNotNumber() throws Exception {

        mockMvc.perform(
                delete("/api/suppliers/abc")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Argument path type is wrong.", response.getErrors());
        });
    }

    @Test
    void deleteFailedSupplierNotFound() throws Exception {

        mockMvc.perform(
                delete("/api/suppliers/123")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Supplier is not found.", response.getErrors());
        });
    }

    @Test
    void deleteSuccess() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("Supplier Test");
        supplier.setPhone("6281238218");
        supplier.setEmail("john@company.xyz");
        supplier.setInformation("Supplier for fishing tools");
        supplierRepository.save(supplier);

        Address address = new Address();
        address.setStreet("JL Test");
        address.setVillage("Village Test");
        address.setDistrict("District Test");
        address.setCity("City Test");
        address.setProvince("Province Test");
        address.setCountry("Country Test");
        address.setPostalCode("0000000");
        address.setSupplier(supplier);
        addressRepository.save(address);

        mockMvc.perform(
                delete("/api/suppliers/" + supplier.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

        });
    }

}